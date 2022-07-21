package kouch.repository

import kouch.KouchClient
import kouch.KouchDesign
import kouch.KouchEntity
import kouch.client.KouchDesignService
import kouch.client.KouchDocument
import kouch.getMetadata
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class KouchRepository<T : KouchEntity, ID : KouchEntity.Id>(
    protected val kouch: KouchClient,
    protected val entityKClass: KClass<T>,
    protected val isErlang: Boolean = true,
) : KouchMigrationContainer {
    protected val highestKey = KouchDocument.HIGHEST_KEY

    suspend fun isDbExist() = kouch.db.isExistFor(entityKClass)
    suspend fun create(entity: T) = kouch.doc.insert(entity, entityKClass)
    suspend fun update(entity: T) = kouch.doc.update(entity, entityKClass)
    suspend fun upsert(entity: T) = kouch.doc.upsert(entity, entityKClass)
    suspend fun delete(entity: T) = kouch.doc.delete(entity, entityKClass)
    suspend fun delete(id: ID, revision: KouchEntity.Rev) = kouch.doc.delete(id, entityKClass, revision)
    suspend fun get(id: ID) = kouch.doc.get(id, entityKClass)
    suspend fun bulkGet(ids: Iterable<ID>) = kouch.db.bulkGet(ids, listOf(entityKClass))
    suspend fun bulkUpsert(
        entities: Iterable<T>,
        entitiesToDelete: Iterable<KouchEntity> = emptyList(),
    ) = kouch.db.bulkUpsert(
        entities = entities,
        entitiesToDelete = entitiesToDelete,
        kClass = entityKClass,
        db = kouch.context.settings.getPredefinedDatabaseName()!!
    )


    protected val metadata = kouch.context.getMetadata(entityKClass)
    protected val defaultDesignDocId = KouchDesign.Id("${metadata.className}_design")
    protected val classField = kouch.context.classField

    protected suspend fun getView(
        id: KouchDesign.Id = defaultDesignDocId,
        viewName: Enum<*>,
        request: KouchDesignService.ViewRequest = KouchDesignService.ViewRequest(
            include_docs = true,
        ),
    ) = kouch.design
        .getView(
            id = id,
            view = KouchDesign.ViewName(viewName.name),
            request = request.copy(include_docs = true),
            resultKClass = entityKClass
        )
        .result
        .filterNotNull()


    open fun views(): Map<out Enum<*>, ViewDefinition> = emptyMap()

    protected fun buildViews(
        views: Map<out Enum<*>, ViewDefinition>,
    ) = KouchMigrationContainer.Migration.UpsertDesign(
        kouchClient = kouch,
        databaseName = metadata.databaseName,
        design = KouchDesign(
            id = defaultDesignDocId,
            language = getLang(),
            views = views.entries.associate {
                KouchDesign.ViewName(it.key.name) to KouchDesign.View(
                    map = it.value.map.trimIndent(),
                    reduce = it.value.reduce?.trimIndent(),
                )
            }
        )
    )

    private fun getLang() = if (isErlang) "Erlang" else "js"

    override fun getMigrations() = listOf(
        buildViews(views())
    )

    protected fun indexByClass() = ViewDefinition(
        map = getIndexByClass()
    )

    protected fun KProperty<*>.asIndex() = "$name".asIndex()

    protected fun String.asIndex() = ViewDefinition(
        map = getIndexByString(this)
    )

    protected fun List<KProperty<*>>.asIndex() = map { "${it.name}" }.asIndex1()

    protected fun List<String>.asIndex1() = when (size) {
        0 -> throw IllegalArgumentException("No fields")
        1 -> single().asIndex()
        else -> ViewDefinition(
            map = getIndexByStrings(this)
        )
    }

    protected fun viewCode(erlangCode: String, jsCode: String) = if (isErlang) {
        erlangCode
    } else {
        jsCode
    }

    private fun getIndexByClass() = if (isErlang) {
        /*language=Erlang*/
        """
            fun({Doc}) ->
                K = proplists:get_value(<<"$classField">>, Doc, null),
                if
                  K == <<"${metadata.className}">> -> Emit(null, null);
                  true -> null
                end
            end.
        """.trimIndent()
    } else {
        /*language=js*/
        """function (doc) {
          if (doc.$classField === '${metadata.className}') {
            emit()
          }
        }
        """
    }

    private fun getIndexByString(name: String) = if (isErlang) {
        /*language=Erlang*/
        """
            fun({Doc}) ->
                K = proplists:get_value(<<"$classField">>, Doc, null),
                P = proplists:get_value(<<"$name">>, Doc, null),
                if
                  K == <<"${metadata.className}">> -> Emit(P, null);
                  true -> null
                end
            end.
        """.trimIndent()
    } else {
        /*language=js*/
        """function (doc) {
          if (doc.$classField === '${metadata.className}') {
            emit(doc.$name, null)
          }
        }
        """
    }

    private fun getIndexByStrings(names: List<String>) = if (isErlang) {
        val props = names.joinToString(",") { "proplists:get_value(<<\"$it\">>, Doc, null)" }
        /*language=Erlang*/
        """
            fun({Doc}) ->
                K = proplists:get_value(<<"$classField">>, Doc, null),
                if
                  K == <<"${metadata.className}">> -> Emit([$props], null);
                  true -> null
                end
            end.
        """.trimIndent()
    } else {
        val props = names.joinToString(",") { "doc.$it" }
        /*language=js*/
        """function (doc) {
                  if (doc.$classField === '${metadata.className}') {
                    emit([${props}], null)
                  }
                }
        """
    }

    interface ViewNameEnum {
        val name: String
    }

    data class ViewDefinition(
        val map: String,
        val reduce: String? = null,
    )

    fun ViewDefinition.withReduce(reduce: String) = copy(
        reduce = reduce
    )
}

class BaseKouchRepository<T : KouchEntity, ID: KouchEntity.Id>(kouch: KouchClient, entityKClass: KClass<T>, isErlang: Boolean = true) :
    KouchRepository<T, ID>(kouch, entityKClass, isErlang)



# kouch

[![](https://jitpack.io/v/lewik/kouch.svg)](https://jitpack.io/#lewik/kouch)

Kotlin multiplatform CouchDB client (JVM, Javascript, common kotlin).
                                      
                                      
## Download
Use https://jitpack.io repository
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Use these dependencies per kotlin module respectively:
```
compile 'com.github.lewik.kouch:kouch-metadata:$kouch_version' //for common modules
compile 'com.github.lewik.kouch:kouch-js:$kouch_version'  //for js modules
compile 'com.github.lewik.kouch:kouch-jvm:$kouch_version'  //for jvm modules
```                                   

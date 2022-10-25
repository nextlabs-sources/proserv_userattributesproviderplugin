# User Attributes Provider Plugin

## Upgrade to Infinispan 13
Major code changes are done in ISCacheManager.java and initializeInfiniSpanCache method of UserAttributesClient.java

Infinispan dependencies are located in scripts/build_compile.xml

## Upgrade dependency versions
RMS SDK Package path is specified in configure file

```
# Dependencies used by to copy java sdk files

if [ "${RMS_SDK_PACKAGE_ZIP}" == "" ]; then
	# JENKINS_URL is set only in a Jenkins build environment
	if [ "X${JENKINS_URL}" != "X" ] ; then
		RMS_SDK_PACKAGE_ZIP="S:/build/pcv/SecureCollaboration/2022.05/414/RMS-2022.05-414-202204250632.zip"
	else
		RMS_SDK_PACKAGE_ZIP="//semakau/Share/build/release_artifacts/Fate/7.7.5.0/2/fate-7.7.5.0-2-release-20160311-bin.zip"
	fi
fi
```

Depedencies copied from rms.war file are updated in scripts/build_xlib.xml

Compile dependencies are updated in scripts/build_compile.xml
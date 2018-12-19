bamboo_JAVA_HOME 	?= /opt/jdk-11 # default to java11 if not set

ifeq ($(MVN),)
	ifeq ($(bamboo_working_directory),) 
		MVN	:= mvn
	else 
		# on bamboo environment define the environment
		export M2_HOME=
		MVN:=/opt/maven-3.3/bin/mvn
		export JAVA_HOME=$(bamboo_JAVA_HOME)
	endif	
endif

export GIT_AUTHOR_NAME=alfresco-build
export GIT_AUTHOR_EMAIL=build@alfresco.com
export GIT_COMMITTER_NAME=alfresco-build
export GIT_COMMITTER_EMAIL=build@alfresco.com

compile:
	$(MVN) --version && $(MVN) --batch-mode compile -U -DskipTests

test: compile
	$(MVN) --batch-mode test -Pdocker-compose || echo "Some tests failed"

environment: ## will install ACS with docker-compose according to profile
	$(MVN) --batch-mode install -DskipTests -Pdocker-compose	

clean:	## will stop ACS docker-compose according to profile
	$(MVN) --batch-mode pre-clean -Pdocker-compose

release: ## perform the release, automatically increase the version 				
	$(MVN) --batch-mode release:prepare release:perform \
	-Dmaven.javadoc.skip=true \
	-Dresume=false \
	-Dusername=$(GIT_COMMITTER_NAME) \
	-Dpassword=${bamboo_git_password} \
	"-Darguments=-Dmaven.test.skip=true -Dmaven.javadoc.skip=true"

### Dataprep
The project aims to provide a collection of helper classes that prepares and loads test data, using Alfresco's API.

### Usage

### a) clone the repository

```shell
   $ git clone https://github.com/AlfrescoTestAutomation/dataprep.git   
```    

### b) run the tests using [Makefile](./Makefile)

```shell
   $ make test
```    
* this will install ACS using docker-compose according to [pom](./pom.xml) profile selected
* and run the API tests over it

### c) cleanup and kill the ACS instance 

```shell
   $ make clean
```    
* this will clean the target folder and kill all docker-compose servicess according to pom profile selected

### release

```shell
   $ make release
``` 
* this will auto-increase the version in `pom.xml` and prepare it for next development iteration.
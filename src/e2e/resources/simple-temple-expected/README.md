# SimpleTempleTest

This directory contains files generated by [Temple](https://templeeight.github.io/temple-docs)  

## Project Structure
* `/api`: API contains the OpenAPI schema for the project
* `/auth`: Backend code for the Auth service
* `/auth-db`: Initialisation scripts for the Auth database
* `/booking`: Backend code for the Booking service
* `/booking-db`: Initialisation scripts for the Booking database
* `/grafana`: Grafana configuration providing a dashboard for each service in the project
* `/kong`: Configuration for the Kong API Gateway
* `/kube`: YAML files for deploying the project with Kubernetes
* `/prometheus`: Prometheus configuration for each service in the project
* `/simple-temple-test-group`: Backend code for the SimpleTempleTestGroup service
* `/simple-temple-test-group-db`: Initialisation scripts for the SimpleTempleTestGroup database
* `/simple-temple-test-user`: Backend code for the SimpleTempleTestUser service
* `/simple-temple-test-user-db`: Initialisation scripts for the SimpleTempleTestUser database
* `deploy.sh`: Deployment script for local development (must be used with `source`)
* `push-image.sh`: Deployment script to push each script to a locally hosted registry

## API Documentation
**Auth Service**  
[Register](#register): `POST /api/auth/register`  
[Login](#login): `POST /api/auth/login`  

**SimpleTempleTestUser Service**  
[List SimpleTempleTestUser](#list-simpletempletestuser): `GET /api/simple-temple-test-user/all`  
[Create SimpleTempleTestUser](#create-simpletempletestuser): `POST /api/simple-temple-test-user`  
[Read SimpleTempleTestUser](#read-simpletempletestuser): `GET /api/simple-temple-test-user/{id}`  
[Update SimpleTempleTestUser](#update-simpletempletestuser): `PUT /api/simple-temple-test-user/{id}`  
[Identify SimpleTempleTestUser](#identify-simpletempletestuser): `GET /api/simple-temple-test-user`  
[List Fred](#list-fred): `GET /api/simple-temple-test-user/{parent_id}/fred/all`  
[Create Fred](#create-fred): `POST /api/simple-temple-test-user/{parent_id}/fred`  
[Read Fred](#read-fred): `GET /api/simple-temple-test-user/{parent_id}/fred/{id}`  
[Update Fred](#update-fred): `PUT /api/simple-temple-test-user/{parent_id}/fred/{id}`  
[Delete Fred](#delete-fred): `DELETE /api/simple-temple-test-user/{parent_id}/fred/{id}`  

**Booking Service**  
[Create Booking](#create-booking): `POST /api/booking`  
[Read Booking](#read-booking): `GET /api/booking/{id}`  
[Delete Booking](#delete-booking): `DELETE /api/booking/{id}`  

**SimpleTempleTestGroup Service**  
[Create SimpleTempleTestGroup](#create-simpletempletestgroup): `POST /api/simple-temple-test-group`  
[Read SimpleTempleTestGroup](#read-simpletempletestgroup): `GET /api/simple-temple-test-group/{id}`  
[Delete SimpleTempleTestGroup](#delete-simpletempletestgroup): `DELETE /api/simple-temple-test-group/{id}`  


***

### Auth

#### Register
**URL:** `/api/auth/register`  
**Method:** `POST`  
**Description:** Register and get an access token  
**Request Contents:**  

|Parameter|Type|Details|
|---|---|---|
|email|String|Valid email address|
|password|String|Between 8 and 64 characters|


##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "AccessToken" : ""
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `403 FORBIDDEN`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Login
**URL:** `/api/auth/login`  
**Method:** `POST`  
**Description:** Login and get an access token  
**Request Contents:**  

|Parameter|Type|Details|
|---|---|---|
|email|String|Valid email address|
|password|String|Between 8 and 64 characters|


##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "AccessToken" : ""
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

### SimpleTempleTestUser Service

#### List SimpleTempleTestUser
**URL:** `/api/simple-temple-test-user/all`  
**Method:** `GET`  
**Description:** Get a list of every simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "SimpleTempleTestUserList" : [
    {
      "breakfastTime" : "23:59:59",
      "lastName" : "string-contents",
      "simpleTempleTestLog" : "string-contents",
      "createdAt" : "2020-01-01T23:59:59",
      "numberOfDogs" : 42,
      "email" : "string-contents",
      "currentBankBalance" : 42.0,
      "firstName" : "string-contents",
      "birthDate" : "2020-01-01",
      "id" : "00000000-0000-0000-0000-000000000000"
    }
  ]
}
```


##### Error Responses:
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Create SimpleTempleTestUser
**URL:** `/api/simple-temple-test-user`  
**Method:** `POST`  
**Description:** Register a new simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  
**Request Contents:**  

|Parameter|Type|Details|
|---|---|---|
|simpleTempleTestLog|String||
|email|String|Min Length: 5, Max Length: 40|
|firstName|String||
|lastName|String||
|createdAt|DateTime|Format: 'YYYY-MM-DDTHH:MM:SS.NNNNNN'|
|numberOfDogs|Integer|Precision: 4 bytes|
|currentBankBalance|Float|Min Value: 0.0, Precision: 2 bytes|
|birthDate|Date|Format: 'YYYY-MM-DD'|
|breakfastTime|Time|Format: 'HH:MM:SS.NNNNNN'|


##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "breakfastTime" : "23:59:59",
  "lastName" : "string-contents",
  "simpleTempleTestLog" : "string-contents",
  "createdAt" : "2020-01-01T23:59:59",
  "numberOfDogs" : 42,
  "email" : "string-contents",
  "currentBankBalance" : 42.0,
  "firstName" : "string-contents",
  "birthDate" : "2020-01-01",
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Read SimpleTempleTestUser
**URL:** `/api/simple-temple-test-user/{id}`  
**Method:** `GET`  
**Description:** Look up a single simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "breakfastTime" : "23:59:59",
  "lastName" : "string-contents",
  "simpleTempleTestLog" : "string-contents",
  "createdAt" : "2020-01-01T23:59:59",
  "numberOfDogs" : 42,
  "email" : "string-contents",
  "currentBankBalance" : 42.0,
  "firstName" : "string-contents",
  "birthDate" : "2020-01-01",
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Update SimpleTempleTestUser
**URL:** `/api/simple-temple-test-user/{id}`  
**Method:** `PUT`  
**Description:** Update a single simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  
**Request Contents:**  

|Parameter|Type|Details|
|---|---|---|
|simpleTempleTestLog|String||
|email|String|Min Length: 5, Max Length: 40|
|firstName|String||
|lastName|String||
|createdAt|DateTime|Format: 'YYYY-MM-DDTHH:MM:SS.NNNNNN'|
|numberOfDogs|Integer|Precision: 4 bytes|
|currentBankBalance|Float|Min Value: 0.0, Precision: 2 bytes|
|birthDate|Date|Format: 'YYYY-MM-DD'|
|breakfastTime|Time|Format: 'HH:MM:SS.NNNNNN'|


##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "breakfastTime" : "23:59:59",
  "lastName" : "string-contents",
  "simpleTempleTestLog" : "string-contents",
  "createdAt" : "2020-01-01T23:59:59",
  "numberOfDogs" : 42,
  "email" : "string-contents",
  "currentBankBalance" : 42.0,
  "firstName" : "string-contents",
  "birthDate" : "2020-01-01",
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Identify SimpleTempleTestUser
**URL:** `/api/simple-temple-test-user`  
**Method:** `GET`  
**Description:** Look up the single simpletempletestuser associated with the access token  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `302 FOUND`  
**Headers:**`Location: http://path/to/resource`  

##### Error Responses:
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### List Fred
**URL:** `/api/simple-temple-test-user/{parent_id}/fred/all`  
**Method:** `GET`  
**Description:** Get a list of every fred belonging to the provided simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "SimpleTempleTestUserList" : [
    {
      "id" : "00000000-0000-0000-0000-000000000000",
      "field" : "string-contents",
      "friend" : "00000000-0000-0000-0000-000000000000",
      "image" : "data"
    }
  ]
}
```


##### Error Responses:
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Create Fred
**URL:** `/api/simple-temple-test-user/{parent_id}/fred`  
**Method:** `POST`  
**Description:** Register a new fred belonging to the provided simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  
**Request Contents:**  

|Parameter|Type|Details|
|---|---|---|
|field|String||
|friend|UUID|Must reference an existing SimpleTempleTestUser|
|image|Base64 String|Max Size: 10000000 bytes|


##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000",
  "field" : "string-contents",
  "friend" : "00000000-0000-0000-0000-000000000000",
  "image" : "data"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Read Fred
**URL:** `/api/simple-temple-test-user/{parent_id}/fred/{id}`  
**Method:** `GET`  
**Description:** Look up a single fred belonging to the provided simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000",
  "field" : "string-contents",
  "friend" : "00000000-0000-0000-0000-000000000000",
  "image" : "data"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Update Fred
**URL:** `/api/simple-temple-test-user/{parent_id}/fred/{id}`  
**Method:** `PUT`  
**Description:** Update a single fred belonging to the provided simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  
**Request Contents:**  

|Parameter|Type|Details|
|---|---|---|
|field|String||
|friend|UUID|Must reference an existing SimpleTempleTestUser|
|image|Base64 String|Max Size: 10000000 bytes|


##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000",
  "field" : "string-contents",
  "friend" : "00000000-0000-0000-0000-000000000000",
  "image" : "data"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Delete Fred
**URL:** `/api/simple-temple-test-user/{parent_id}/fred/{id}`  
**Method:** `DELETE`  
**Description:** Delete a single fred belonging to the provided simpletempletestuser  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

### Booking Service

#### Create Booking
**URL:** `/api/booking`  
**Method:** `POST`  
**Description:** Register a new booking  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Read Booking
**URL:** `/api/booking/{id}`  
**Method:** `GET`  
**Description:** Look up a single booking  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Delete Booking
**URL:** `/api/booking/{id}`  
**Method:** `DELETE`  
**Description:** Delete a single booking  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

### SimpleTempleTestGroup Service

#### Create SimpleTempleTestGroup
**URL:** `/api/simple-temple-test-group`  
**Method:** `POST`  
**Description:** Register a new simpletempletestgroup  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Read SimpleTempleTestGroup
**URL:** `/api/simple-temple-test-group/{id}`  
**Method:** `GET`  
**Description:** Look up a single simpletempletestgroup  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{
  "id" : "00000000-0000-0000-0000-000000000000"
}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

#### Delete SimpleTempleTestGroup
**URL:** `/api/simple-temple-test-group/{id}`  
**Method:** `DELETE`  
**Description:** Delete a single simpletempletestgroup  
**Auth:** Include Authorization header in format `Authorization: Bearer <token>`  

##### Success Response:
**Code:** `200 OK`  
**Response Body:**  
```
{}
```


##### Error Responses:
**Code:** `400 BAD REQUEST`  
**Code:** `401 UNAUTHORIZED`  
**Code:** `404 NOT FOUND`  
**Code:** `500 INTERNAL SERVER ERROR`  


***

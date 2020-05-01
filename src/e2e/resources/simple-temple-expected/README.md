# SimpleTempleTest


## API Documentation

### Auth

#### Register
**URL:** `/api/register`  
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
**URL:** `/api/login`  
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

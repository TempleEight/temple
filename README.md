# Temple

[![codecov](https://codecov.io/gh/TempleEight/temple/branch/develop/graph/badge.svg)](https://codecov.io/gh/TempleEight/temple)
![](https://img.shields.io/travis/com/templeeight/temple)
![](https://img.shields.io/github/license/templeeight/temple)

Visit https://templeeight.github.io/temple-docs/ for full guides and documentation.

Temple is a software framework for generating and deploying scalable microservice infrastructure.
It provides developers an easily extensible foundation on which to add bespoke logic, suited to their business requirements.

Temple provides a high-level Domain Specific Language (DSL), allowing you to describe your desired data model, along with any constratints. 

From this description, Temple generates:
- RESTful backend microservice code, with data manipulation endpoints (CRUD and List) and hooks to extend with business logic. 
- Database/datastore configuration scripts.
- An [OpenAPI](https://www.openapis.org) specification for the generated microservices
- Dockerfiles to containerise our generated services.
- Deployment scripts for local development, either using Kubernetes or Docker Compose
- Automatic load balancing with [Kong](https://konghq.com) 

In addition to this, the microservices can optionally be augmented with:
- systems for security, including JWT authentication
- usage metrics, providing critical information about success, failure and latency

Full documentation can be found at https://templeeight.github.io/temple-docs/

## Installation

For installation information, see https://templeeight.github.io/temple-docs/docs/installation

## License
Apache License 2.0, see [LICENSE](https://github.com/templeeight/temple/blob/master/LICENSE)

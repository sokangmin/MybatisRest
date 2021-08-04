# Rest4Mybatis
Rest4Mybatis는 사용자가 MyBatis mapper파일에 정의한 SQL정보를 토대로 RESTful에 가까운 API 서비스를 생성하는 Generator입니다. &nbsp; 또한 웹화면으로 REST API에 대한 문서화 및 api테스트도구 등 개발에 편리한 기능을 제공하고 있습니다. 
<image src='./rest_mybatis.png' width='35%' height='35%'/><br/>

## 개발 배경
- Mybatis, Spring MVC를 기반으로 REST API 서버를 개발하는 경우, Mybatis의 mapper파일이 변경이 될때 마다 Controller, Entity, Repository(dao), Service 등 3~4개의 클래스 및 인터페이스를 수정해야 하고 서비스가 늘어날때 마다 그에 따른 관리 포인트도 늘어남.
- 개발한 REST API에 대한 문서화 및 테스트 도구 필요.


# Rest4MyBatis
Rest4Mybatis는 사용자가 MyBatis mapper파일에 정의한 SQL정보를 토대로 RESTful에 가까운 API 서비스를 생성하는 Generator입니다. &nbsp; 또한 웹화면으로 REST API에 대한 문서화 및 api테스트도구 등 개발에 편리한 기능을 제공하고 있습니다. 

<image src='./rest_mybatis.png?raw=true' width='30%' height='30%'/><br/>

## 개발 배경
- Mybatis, Spring MVC를 기반으로 REST API 서버를 개발하는 경우, Mybatis의 mapper파일이 변경이 될때 마다 Controller, Entity, Repository(dao), Service 등 3~4개의 클래스 및 인터페이스를 수정해야 하고 서비스가 늘어날때 마다 그에 따른 관리 포인트도 늘어남.
- 개발한 REST API에 대한 문서화 및 테스트 도구 필요.

## 특징
```xml
<!-- Mybatis Mapper 파일 예제 -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "mybatis-3-mapper.dtd">
<mapper namespace="mybatis.mapper.test">

<!-- url : /mybatis/mapper/test/tableA?id=0, method : GET -->
<select id="select_tableA" parameterType="string" resultType="HashMap" fetchSize="1">
  SELECT  id, name
  FROM    tableA
  WHERE   id = #{id}
</select>

<!-- url : /mybatis/mapper/test/tableA, method : POST -->
<!-- body : {"name":"test","datetime":"20210804130000"} Content-type : application/json; charset=utf-8 -->
<insert id="insert_tableA" parameterType="HashMap">
  <selectKey keyProperty="id" resultType="string" order="BEFORE">
    <!-- sql -->
  </selectKey>
  INSERT
  INTO    tableA
          (
            id,
            name,
            datetime
          )
  VALUES
          (
            #{id},
            #{name},
            to_timestamp(#{datetime}, 'yyyymmddhh24miss')
          )
</insert>

<!-- url : /mybatis/mapper/test/fn_get_cd_nm, method : GET -->
<select id="fn_get_cd_nm" parameterType="string" resultType="HashMap">
  <!-- sql -->
</select>
```
- JDBC를 지원하는 데이터베이스는 모두 적용하고 Mybatis에서 제공하는 기능들은 대부분 지원함.
- Mybatis mapper파일의 각 구문의 명칭은 HTTP Method와 아래와 같이 맵핑됨.<br/>

  | Mybatis 구문 | HTTP Method |
  |:--------:|:--------:|
  | SELECT | GET |
  | INSERT | POST |
  | UPDATE | PUT |
  | DELETE | DELETE |
- Mybatis mapper파일의 namespace와 id를 사용하여 REST API URL로 활용.<br/>
  각 구문의 명칭과 id의 접두사가 일치하는 경우 '접두사_'는 url에서 제거됨.
- Mybatis mapper파일의 select, delete 구문의 parameter는 HTTP URL의 쿼리스트링으로 맵핑됨.</br>
  insert, update 구문의 parameter는 HTTP의 body문으로 맵핑이 되고 json으로 작성. (Content-Type: application/json; charset=utf-8)
- parameterType, resultType 등에 사용자정의 클래스는 사용 안됨. 사용자정의 클래스는 HashMap 또는 [사용자정의 ResultMap](./image006.png?raw=true) 으로 수정해서 사용해야 함.
- Select 구문은 default로 json array로 리턴함.
  fetchSize를 1로 하면 json으로 리턴함.
- Insert 실행 성공시, 등록한 keyProperty에 해당하는 값을 리턴함.
  update, delete 실행시 {"result": true} 리턴함.
- SQL이 정상적으로 수행시 HTTP 상태코드 200, 잘못된 요청일 경우 400번대, 에러 발생시 500 및 에러내용을 리턴함.
- api 문서화 및 테스트 도구 지원

  | select_tableA | insert_tableA | fn_get_cd_nm |
  |:--------:|:--------:|:--------:|
  | <image src='./image001.png?raw=true' width='50%' height='50%'/> | <image src='./image002.png?raw=true' width='40%' height='40%'/> | <image src='./image003.png?raw=true' width='50%' height='50%'/> |
  
## 설치 및 실행
- 폴더구조<br/>
  <image src='./image004.png?raw=true' width='50%' height='50%'/>
  - MybatisRest-0.0.1-SNAPSHOT.jar : java 클래스, lib, 메타데이터 등이 모여있는 파일
  - config : 환경설정 폴더
    - application.yml : SpringBoot 환경설정파일
    - mapper : Mybatis mapper 파일이 모여있는 폴더(위치이동 가능함.)
- application.yml 설정
  ```yaml
  # http 서버 포트 정보
  server:
    port: 8080

  # Database 정보
  spring:
    datasource:
      driver-class-name: org.postgresql.Driver
      url: jdbc:postgresql://localhost:5432/postgres
      username: ermct
      password: ermct00
    
  # Mybatis     
  mybatis:
    mapper-locations: classpath:/mapper/**/*.xml
    
  # 그외 설정정보는 SpringBoot 참고
  ```
- 실행방법
  - java 1.8 이상 필요.
  - java -jar MybatisRest-0.0.1-SNAPSHOT.jar 로 실행.
  - http://{서버ip}:{서버port}/docs 로 접속해서 api 테스트화면이 나타나면 정상. 
## 활용사례
- 5G기반 인공지능(AI) 응급의료서비스
  - AI 기반 응급의료시스템은 응급현장에서 환자의 생체데이터를 5G 망을 통해 병원으로 전송하고 통합플랫폼에서 분석된 정보를 토대로
    응급현장의 구급대원에게 응급처치방안 및 최적의 병원과 이송경로를 제시하는 시스템이다.
  - 응급현장과 통합플랫폼 사이의 API  연계모듈의 데이터베이스 SERVICE를 이 프로그램을 사용하여 구현함.
  <image src='./image005.png?raw=true' width='35%' height='35%'/><br/>

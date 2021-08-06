# Rest4Mybatis
Rest4Mybatis는 사용자가 MyBatis mapper파일에 정의한 SQL정보를 토대로 RESTful에 가까운 API 서비스를 생성하는 Generator입니다. &nbsp; 또한 웹화면으로 REST API에 대한 문서화 및 api테스트도구 등 개발에 편리한 기능을 제공하고 있습니다. 

<image src='./rest_mybatis.png' width='30%' height='30%'/><br/>

## 개발 배경
- Mybatis, Spring MVC를 기반으로 REST API 서버를 개발하는 경우, Mybatis의 mapper파일이 변경이 될때 마다 Controller, Entity, Repository(dao), Service 등 3~4개의 클래스 및 인터페이스를 수정해야 하고 서비스가 늘어날때 마다 그에 따른 관리 포인트도 늘어남.
- 개발한 REST API에 대한 문서화 및 테스트 도구 필요.

## 특징
```xml
<!-- mapper 파일 예제 -->
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
- JDBC를 지원하는 데이터베이스는 모두 적용가능함.
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
- parameterType, resultType 등에 사용자정의 클래스는 사용 안됨. 사용자정의 클래스는 HashMap으로 수정해서 사용해야 함.
- Select 구문은 default로 json array로 리턴함.
  fetchSize를 1로 하면 json으로 리턴함.
- Insert 실행 성공시, 등록한 keyProperty에 해당하는 값을 리턴함.
  update, delete 실행시 {"result": true} 리턴함.
- SQL이 정상적으로 수행시 HTTP 상태코드 200을 리턴하고 에러 발생시 500 및 에러내용을 리턴함.
- api 문서화 및 테스트 도구 지원

  | select_tableA | insert_tableA |
  |:--------:|:--------:|
  | <image src='./image001.png' width='50%' height='50%'/> | <image src='./image001.png' width='50%' height='50%'/> |
  

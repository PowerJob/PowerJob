package tech.powerjob.common.serialize;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * test json utils
 *
 * @author tjq
 * @since 2024/3/16
 */
@Slf4j
class JsonUtilsTest {

    @Test
    @SneakyThrows
    void simpleTest() {
        Person person = new Person().setName("mubao").setAge(18);
        String jsonString = JsonUtils.toJSONString(person);
        log.info("[JsonUtilsTest] person: {}, jsonString: {}", person, jsonString);
        assert jsonString != null;
        Person person2 = JsonUtils.parseObject(jsonString, Person.class);
        assert person2.equals(person);
    }

    @Test
    @SneakyThrows
    void testAdvanceApi() {
        PersonPlus personPlus = new PersonPlus();
        personPlus.setName("gongbao").setAge(3);
        personPlus.setBirthDay(LocalDateTime.now());

        String jsonString = JsonUtils.toJSONString(personPlus);
        PersonPlus personPlus2 = JsonUtils.parseObject(jsonString, PersonPlus.class);
        assert personPlus2.equals(personPlus);
    }

    @Test
    @SneakyThrows
    void testMoreOrLessFields() {
        PersonPlus personPlus = new PersonPlus().setBirthDay(LocalDateTime.now());
        personPlus.setName("gongbao").setAge(3);

        String originJsonStr = JsonUtils.toJSONString(personPlus);

        Map<String, Object> personPlusMapMore = JsonUtils.parseMap(originJsonStr);
        personPlusMapMore.put("extraKey", System.currentTimeMillis());

        PersonPlus personPlusByMoreFieldsJsonStr = JsonUtils.parseObject(JsonUtils.toJSONString(personPlusMapMore), PersonPlus.class);
        assert personPlusByMoreFieldsJsonStr.equals(personPlus);

        Map<String, Object> personPlusMapLess = JsonUtils.parseMap(originJsonStr);
        personPlusMapLess.remove("birthDay");

        PersonPlus personPlusByLessFieldsJsonStr = JsonUtils.parseObject(JsonUtils.toJSONString(personPlusMapLess), PersonPlus.class);
        assert personPlusByLessFieldsJsonStr.getName().equals(personPlus.getName());
        assert personPlusByLessFieldsJsonStr.getAge().equals(personPlus.getAge());
    }

    @Data
    @Accessors(chain = true)
    static class Person implements Serializable {
        private String name;
        private Integer age;
    }

    @Data
    @Accessors(chain = true)
    @EqualsAndHashCode(callSuper = true)
    static class PersonPlus extends Person {
        private LocalDateTime birthDay;
    }
}
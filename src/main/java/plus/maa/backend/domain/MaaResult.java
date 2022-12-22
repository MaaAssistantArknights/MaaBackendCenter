package plus.maa.backend.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maa的响应类，注意不要加Lombok的注解，格式已适配前端的请求
 *
 * @author AnselYuki
 */
public class MaaResult {
    private final Map<Object, Object> result;
    private final String code;

    public MaaResult(int code, String msg) {
        this.code = String.valueOf(code);
        Map<Object, Object> result = new HashMap<>(3);
        result.put("status_code", code);
        result.put("message", msg);
        this.result = result;
    }

    public MaaResult(int code, String msg, Object data) {
        this.code = String.valueOf(code);
        Map<Object, Object> result = new HashMap<>(3);
        result.put("status_code", code);
        result.put("message", msg);
        result.put("data", data);
        this.result = result;
    }

    /**
     * 动态修改json数组的键名，要返回的数据记得封装在result内
     * 由于前端的接口请求格式，所有数据都以httpCode为键，作为一个单集合被序列化
     *
     * @return 修改完毕的对象
     */
    @JsonAnyGetter
    public Map<String, Object> getResultMap() {
        return Collections.singletonMap(code, result);
    }
}
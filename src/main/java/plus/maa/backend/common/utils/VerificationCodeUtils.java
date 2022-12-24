package plus.maa.backend.common.utils;

import java.util.Random;

/**
 * @author LoMu
 * Date  2022-12-24 11:40
 */
public class VerificationCodeUtils {




    /**
     *
     * @return 随机生成六位长度字符串验证码
     */
    public static String generateVCode6LString() {
        return generateValidateCodeCustomLString(6);
    }



    /**
     *
     *
     * @return 随机生成验证码长度为6位数字
     */
    public static Integer generateValidateCode6LInteger(){
        int code = new Random().nextInt(999999);//生成随机数，最大为999999
        if(code < 100000){
            code = code + 100000;//保证随机数为6位数字
        }
        return code;
    }

    /**
     *
     * @param length 长度
     * @return 随机生成指定长度字符串验证码
     */
    public static String generateValidateCodeCustomLString(int length){
        Random rdm = new Random();
        String hash = Integer.toHexString(rdm.nextInt());
        return hash.substring(0, length);
    }
}

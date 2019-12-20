package com.tongji.meeting.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tongji.meeting.util.redis.RedisUtils;
import org.apache.shiro.codec.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class WechatUtil {

    private static RedisUtils redisUtils;

    @Autowired
    public WechatUtil(RedisUtils redisUtils) {
        WechatUtil.redisUtils = redisUtils;
    }

    public static JSONObject getSessionKeyOrOpenId(String code) {
        String requestUrl = "https://api.weixin.qq.com/sns/jscode2session";
        Map<String, String> requestUrlParam = new HashMap<>();
        //小程序appId
        requestUrlParam.put("appid", GlobalValues.appId);
        //小程序secret
        requestUrlParam.put("secret", GlobalValues.appSecret);
        //小程序端返回的code
        requestUrlParam.put("js_code", code);
        //默认参数
        requestUrlParam.put("grant_type", "authorization_code");
        //发送post请求读取调用微信接口获取openid用户唯一标识
        JSONObject jsonObject = JSON.parseObject(HttpClientUtil.doPost(requestUrl, requestUrlParam));
        return jsonObject;
    }

    private static JSONObject getAccessToken(){
        String requestUrl = "https://api.weixin.qq.com/cgi-bin/token";
        Map<String, String> requestUrlParam = new HashMap<>();
        requestUrlParam.put("appid", GlobalValues.appId);
        requestUrlParam.put("secret", GlobalValues.appSecret);
        requestUrlParam.put("grant_type", "client_credential");
        JSONObject jsonObject = JSON.parseObject(HttpClientUtil.doGet(requestUrl, requestUrlParam));
        return jsonObject;
    }

    public static JSONObject getUserInfo(String encryptedData, String sessionKey, String iv) {
        // 被加密的数据
        byte[] dataByte = Base64.decode(encryptedData);
        // 加密秘钥
        byte[] keyByte = Base64.decode(sessionKey);
        // 偏移量
        byte[] ivByte = Base64.decode(iv);
        try {
            // 如果密钥不足16位，那么就补足.  这个if 中的内容很重要
            int base = 16;
            if (keyByte.length % base != 0) {
                int groups = keyByte.length / base + (keyByte.length % base != 0 ? 1 : 0);
                byte[] temp = new byte[groups * base];
                Arrays.fill(temp, (byte) 0);
                System.arraycopy(keyByte, 0, temp, 0, keyByte.length);
                keyByte = temp;
            }
            // 初始化
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            SecretKeySpec spec = new SecretKeySpec(keyByte, "AES");
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
            parameters.init(new IvParameterSpec(ivByte));
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);// 初始化
            byte[] resultByte = cipher.doFinal(dataByte);
            if (null != resultByte && resultByte.length > 0) {
                String result = new String(resultByte, "UTF-8");
                return JSON.parseObject(result);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean sendMessage(String access_token, String openid, String template_id, JSONObject data, String page){
        String requestUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=";
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("touser", openid);
        jsonRequest.put("template_id", template_id);
        jsonRequest.put("data", data);
        if (page != null){
            jsonRequest.put("page", page);
        }
        JSONObject jsonObject = JSON.parseObject(HttpClientUtil.doPostJson(requestUrl+access_token, jsonRequest.toJSONString()));
        if (jsonObject.containsKey("errcode")&&jsonObject.getInteger("errcode")!=0){
            System.out.println(jsonObject.getString("errmsg"));
            return false;
        }
        return true;
    }

    public static boolean sendMessage(String access_token, String openId, String template_id, JSONObject data){
        return sendMessage(access_token, openId, template_id, data, null);
    }

    public static boolean sendMessage(String openId, String template_id, JSONObject data){
        return sendMessage(getAccessTokenInRedis(), openId, template_id, data);
    }

    public static void updateAccessTokenInRedis(){
        JSONObject data = WechatUtil.getAccessToken();
        if (data.containsKey("errcode")&&data.getInteger("errcode")!=0){
            System.out.println(data.getString("errmsg"));
        }else {
            String access_token = data.getString("access_token");
            long expires_in = data.getLong("expires_in");
            redisUtils.set("access_token", access_token, expires_in);
        }
    }

    public static String getAccessTokenInRedis(){
        if (!redisUtils.hasKey("access_token")){
            updateAccessTokenInRedis();
        }
        return  (String)redisUtils.get("access_token");
    }

}


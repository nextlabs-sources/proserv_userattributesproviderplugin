package com.nextlabs.plugins.userattributes.v2;

import com.nextlabs.common.shared.JsonIdentityProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.infinispan.jboss.marshalling.commons.GenericJBossMarshaller;

import com.nextlabs.common.util.Hex;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ISCacheManager {

    private static final String IDP_CACHE = "IDP_CACHE";
    private static final String USER_CACHE = "USER_CACHE";
    private static final String APPLOGIN_NONCE_CACHE = "APPLOGIN_NONCE_CACHE";
    // cache to store mapping between token group name and keystore id, for easy retrieval for eval in viewer and rms
    private static final String TOKEN_GROUP_KEYSTORE_MAP_CACHE = "TOKEN_GROUP_MAP_CACHE";
    private static final String EAP_CACHE = "EAP_CACHE";
    private static final String EAP_ATTR_CACHE = "EAP_ATTR_CACHE";
    private static final String SESSION_CACHE = "SESSION_CACHE";

    private static ISCacheManager instance;
    private final long startedAt;
    private BasicCacheContainer manager;

    private final BasicCache<String, List<JsonIdentityProvider>> idpCache;
    private final BasicCache<String, UserAttributeCacheItem> userAttributeCache;
    private final BasicCache<String, String> loginNonceCache;
    private final BasicCache<String, String> tokenGroupCache;
    private final BasicCache<String, List<Object>> eapCache;
    private final BasicCache<String, HashMap<String, Integer>> eapAttrCache;
    private final BasicCache<String, String> sessionCache;

    private ISCacheManager(Properties hotrodProp, Logger logger) throws IOException {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.withProperties(hotrodProp);
        builder.marshaller(new GenericJBossMarshaller());

        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());
        manager = cacheManager;

        // cache names must match those in infinispan_server.xml
        idpCache = cacheManager.getCache(IDP_CACHE);
        userAttributeCache = cacheManager.getCache(USER_CACHE);
        loginNonceCache = cacheManager.getCache(APPLOGIN_NONCE_CACHE);
        tokenGroupCache = cacheManager.getCache(TOKEN_GROUP_KEYSTORE_MAP_CACHE);
        eapCache = cacheManager.getCache(EAP_CACHE);
        eapAttrCache = cacheManager.getCache(EAP_ATTR_CACHE);
        sessionCache = cacheManager.getCache(SESSION_CACHE);
        if (idpCache == null || userAttributeCache == null || loginNonceCache == null || tokenGroupCache == null || eapCache == null || eapAttrCache == null || sessionCache == null) {
            throw new IOException("Error in initializing Remote Infinispan Client");
        }
        logger.info("Initialized Remote Infinispan REST Client for " + hotrodProp.getProperty("infinispan.client.hotrod.server_list"));
        startedAt = System.currentTimeMillis();
    }

    public static ISCacheManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Infinispan Cache Manager is not initialized.");
        }
        return instance;
    }

    public static synchronized void init(Properties hotrodProp, Logger logger) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("Infinispan Cache Manager is already initialized.");
        }
        instance = new ISCacheManager(hotrodProp, logger);
    }

    public void shutdown() {
        if (manager != null) {
            manager.stop();
        }
    }

    public long getstartedAt() {
        return startedAt;
    }

    public BasicCache<String, List<JsonIdentityProvider>> getIdpCache() {
        return idpCache;
    }

    public BasicCache<String, UserAttributeCacheItem> getUserAttributeCache() {
        return userAttributeCache;
    }

    public BasicCache<String, String> getAppLoginNonceCache() {
        return loginNonceCache;
    }

    public BasicCache<String, String> getTokenGroupCache() {
        return tokenGroupCache;
    }

    public BasicCache<String, List<Object>> getEapCache() {
        return eapCache;
    }

    public BasicCache<String, HashMap<String, Integer>> getEapAttrCache() {
        return eapAttrCache;
    }

    public BasicCache<String, String> getSessionCache() {
        return sessionCache;
    }
}

class UserAttributeCacheItem implements Serializable {

    private static final long serialVersionUID = 1845197891352327881L;
    private static final String ENCRYPT_KEY = "6p4oc2qEJiM2pBQZ";
    public static final String EMAIL = "email";
    public static final String DISPLAYNAME = "displayName";
    public static final String ADUSERNAME = "aduser";
    public static final String ADPASS = "adpass";
    public static final String ADDOMAIN = "addomain";
    public static final String UNIQUE_ID_ATTRIBUTE = "idp_unique_id";
    public static final String ATTRIBUTE_TYPE = "attrtype";
    public static final String ATTRIBUTE_INDEX = "attrindex";
    public static final int STRING_VALUE = 0;
    public static final int MULTI_VALUE = 1;
    public static final int NUMBER_VALUE = 2;
    private Map<String, List<String>> userAttributes;

    public Map<String, List<String>> getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(Map<String, List<String>> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public static String getKey(int userId, String clientId) {
        return new StringBuilder(40).append(userId).append('@').append(clientId).toString();
    }

    public static String encrypt(String value) throws GeneralSecurityException {
        byte[] raw = ENCRYPT_KEY.getBytes(Charset.forName("UTF-8"));
        Key skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParams);
        return Hex.toHexString(cipher.doFinal(value.getBytes(Charset.forName("UTF-8"))));
    }

    public static String decrypt(String encrypted) throws GeneralSecurityException {
        byte[] raw = ENCRYPT_KEY.getBytes(Charset.forName("UTF-8"));
        Key key = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] ivByte = new byte[cipher.getBlockSize()];
        IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, key, ivParamsSpec);
        return new String(cipher.doFinal(Hex.toByteArray(encrypted)), StandardCharsets.UTF_8);
    }
}

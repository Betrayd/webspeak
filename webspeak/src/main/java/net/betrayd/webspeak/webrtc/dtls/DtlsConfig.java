package net.betrayd.webspeak.webrtc.dtls;

import lombok.val;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.tls.CipherSuite;
import org.jitsi.metaconfig.ConfigException;

import java.time.Duration;
import java.util.List;

public class DtlsConfig {
    public static final String FINGERPRINT_HASH_FUNCTION = "sha-256";

    /**
     * TODO: eventually also put this in a config file.
     */
    public static Duration getTimeout(){
        return Duration.ofSeconds(30);
    }

    /**
     * TODO: make in the config because your supposed to
     */
    public static List<Integer> getSupportedCipherSuites(){
        return List.of(
                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
        );
    }

    /**
     * TODO: make in the config because your supposed to
     */
    public static List<String> getAcceptedFingerprintHashFunctions(){
        return List.of("sha-256");
    }

    /**
     * For eventually making the fingerpirnt hashFunction come from the config file if we do that
     * <p>I already translated it is all</p>
     */
    private String validateHashFunction(String func) throws ConfigException {
        val ucFunc = func.toUpperCase();
        if(DefaultDigestAlgorithmIdentifierFinder.INSTANCE.find(ucFunc) != null){
            throw new ConfigException.UnableToRetrieve.WrongType("Unknown hash function" + func);
        }
        if (ucFunc.equals("MD5") || ucFunc.equals("MD2")) {
            throw new  ConfigException.UnableToRetrieve.WrongType("Forbidden hash function" + func);
        }
        return func.toLowerCase();
    }
}

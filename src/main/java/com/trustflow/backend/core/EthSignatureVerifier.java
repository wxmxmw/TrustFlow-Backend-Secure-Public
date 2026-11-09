package com.trustflow.backend.core;

import org.springframework.stereotype.Component;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class EthSignatureVerifier {
    public boolean verifyPersonalSign(String address, String message, String signatureHex) {
        byte[] sig = Numeric.hexStringToByteArray(signatureHex);
        if (sig.length != 65)
            return false;
        byte v = sig[64];
        if (v < 27)
            v += 27;
        byte[] r = Arrays.copyOfRange(sig, 0, 32);
        byte[] s = Arrays.copyOfRange(sig, 32, 64);
        Sign.SignatureData sd = new Sign.SignatureData(v, r, s);
        BigInteger pubKey;
        try {
            pubKey = Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8), sd);
        } catch (Exception e) {
            return false;
        }
        String recovered = "0x" + Keys.getAddress(pubKey);
        return recovered.equalsIgnoreCase(address);
    }
}

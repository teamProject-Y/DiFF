//package com.example.demo.service;
//
//import com.example.demo.repository.OAuthAccountRepository;
//import com.example.demo.vo.OAuthAccount;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class OAuthAccountService {
//
//    @Autowired
//    private OAuthAccountRepository oAuthAccountRepository;
//
//    public OAuthAccountService(OAuthAccountRepository oAuthAccountRepository) {
//        this.oAuthAccountRepository = oAuthAccountRepository;
//    }
//
//    public OAuthAccount findByProviderAndOauthId(String provider, String oauthId) {
//        return oAuthAccountRepository.findByProviderAndOauthId(provider, oauthId);
//    }
//
//    public OAuthAccount findByOauthId(String oauthId) {
//        return oAuthAccountRepository.findByOauthId(oauthId);
//    }
//}

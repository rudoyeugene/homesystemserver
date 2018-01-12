package com.rudyii.hsw.simple;

import com.google.gson.Gson;
import com.rudyii.hsw.objects.User;
import com.rudyii.hsw.objects.WanIp;
import org.apache.maven.shared.utils.io.IOUtil;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class SimpleTestsCollection {

    @Test
    public void runMe()  {
        User user = new User();

        user.setEmail("bla");
        user.setFcmToken("blabla");
        user.setaField("bla-bla");

        Gson gson = new Gson();
        String jsonString = gson.toJson(user);

        System.out.println(jsonString);

        User userNew = gson.fromJson(jsonString, User.class);
        System.out.println(userNew.getEmail());
        System.out.println(userNew.getFcmToken());
    }

    @Test
    public void wanIpObjectFromJsonString() throws Exception {
        String whatsMyIpJson = "http://ip-api.com/json";
        Gson gson = new Gson();

        String response = IOUtil.toString(new URL(whatsMyIpJson).openStream());

        WanIp wanIp = gson.fromJson(response, WanIp.class);

        System.out.println(wanIp.toString());
    }

    @Test
    public void arrays(){
        ArrayList<String> strings = new ArrayList<>();
        strings.add("one");
        strings.add("two");
        strings.add("three");

        String convertedStrings = Arrays.toString(strings.toArray());
        System.out.println(convertedStrings);

        ArrayList<String> strings2 = new ArrayList<>(Arrays.asList(convertedStrings.split("")));

        System.out.println((512000 * 100)/916000 + "%");
    }
}

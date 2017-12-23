import com.google.gson.Gson;
import com.rudyii.hsw.objects.User;
import com.rudyii.hsw.objects.WanIp;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

import java.net.URL;

public class SimpleTest {

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
}

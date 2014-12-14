package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClients;

public class Spotify {

    static void post() {

        HttpClient httpclient = HttpClients.createDefault();

        HttpPost httppost = new HttpPost("https://ws.spotify.com/oauth/token");

    }


}

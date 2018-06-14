package com.rudyii.hsw.providers;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DropboxClientProvider {

    private DbxClientV2 client;

    @Value("#{hswProperties['dropbox.token']}")
    private String dropboxToken;

    @Bean
    public DbxClientV2 getDbxClientV2() {
        if (client == null) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("HomeSystem").build();
            this.client = new DbxClientV2(config, dropboxToken);
        }
        return client;
    }
}

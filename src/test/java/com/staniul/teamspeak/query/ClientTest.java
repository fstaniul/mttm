package com.staniul.teamspeak.query;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import org.junit.Test;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

public class ClientTest {
    @Test
    public void createClientTest () throws Throwable {
//        Map<String, String> info = JTS3ServerQuery.parseLine("cfid=0 ctid=119 reasonid=0 clid=27 client_unique_identifier=YEn9xSTVgn5WoRLDiI+X+Ukefjw= client_nickname=Pingwin client_input_muted=0 client_output_muted=0 client_outputonly_muted=0 client_input_hardware=1 client_output_hardware=1 client_meta_data client_is_recording=0 client_database_id=40028 client_channel_group_id=8 client_servergroups=8 client_away=0 client_away_message client_type=0 client_flag_avatar client_talk_power=0 client_talk_request=0 client_talk_request_msg client_description client_is_talker=0 client_is_priority_speaker=0 client_unread_messages=0 client_nickname_phonetic client_needed_serverquery_view_power=75 client_icon_id=0 client_is_channel_commander=0 client_country=PL client_channel_group_inherited_channel_id=119 client_badges=Overwolf=0");
//        info.putIfAbsent("cid", info.get("ctid"));
        Map<String, String> info = JTS3ServerQuery.parseLine("cid=13619 client_idle_time=88 client_unique_identifier=YEn9xSTVgn5WoRLDiI+X+Ukefjw= client_nickname=Pingwin client_version=3.0.19.4\\s[Build:\\s1468491418] client_platform=Windows client_input_muted=0 client_output_muted=0 client_outputonly_muted=0 client_input_hardware=1 client_output_hardware=1 client_default_channel client_meta_data client_is_recording=0 client_version_sign=ldWL49uDKC3N9uxdgWRMTOzUuiG1nBqUiOa+Nal5HvdxJiN4fsTnmmPo5tvglN7WqoVoFfuuKuYq1LzodtEtCg== client_security_hash client_login_name client_database_id=40028 client_channel_group_id=8 client_servergroups=8 client_created=1493734750 client_lastconnected=1497527798 client_totalconnections=6 client_away=0 client_away_message client_type=0 client_flag_avatar client_talk_power=0 client_talk_request=0 client_talk_request_msg client_description client_is_talker=0 client_month_bytes_uploaded=0 client_month_bytes_downloaded=216380 client_total_bytes_uploaded=0 client_total_bytes_downloaded=843928 client_is_priority_speaker=0 client_nickname_phonetic client_needed_serverquery_view_power=75 client_default_token client_icon_id=0 client_is_channel_commander=0 client_country=PL client_channel_group_inherited_channel_id=13619 client_badges=Overwolf=0 client_base64HashClientUID=gaejpnmfcenfichofgkbbcmdiiipjhpjejbohodm connection_filetransfer_bandwidth_sent=0 connection_filetransfer_bandwidth_received=0 connection_packets_sent_total=13656 connection_bytes_sent_total=1999558 connection_packets_received_total=29887 connection_bytes_received_total=4488190 connection_bandwidth_sent_last_second_total=7841 connection_bandwidth_sent_last_minute_total=5327 connection_bandwidth_received_last_second_total=7735 connection_bandwidth_received_last_minute_total=7861 connection_connected_time=577055 connection_client_ip=93.105.1.93");
        Client client = new Client(27, info);
        Field[] fields = Client.class.getDeclaredFields();
        for (Field field : fields) {
            boolean acc =field.isAccessible();
            if (!acc) field.setAccessible(true);
            Object o = field.get(client);
            assertNotNull(o);
            field.setAccessible(acc);
        }
    }
}
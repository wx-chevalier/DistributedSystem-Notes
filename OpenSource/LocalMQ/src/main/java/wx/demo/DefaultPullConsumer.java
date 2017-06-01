package wx.demo;

import io.openmessaging.KeyValue;
import wx.mq.embedded.client.EmbeddedPullConsumer;
import wx.mq.local.client.LocalProducer;
import wx.mq.local.client.LocalPullConsumer;

public class DefaultPullConsumer extends LocalPullConsumer {

    public DefaultPullConsumer(KeyValue properties) {
        super(properties);
    }

    public DefaultPullConsumer(KeyValue properties, int refId) {
        super(properties,refId);
    }

}

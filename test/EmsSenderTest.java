package com.banamex.cops.update_offer.sender;

import jakarta.jms.BytesMessage;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmsSenderTest {

    @Mock JmsTemplate jmsTemplate;

    private EmsSender sender;
    private Session session;
    private TemporaryQueue replyQueue;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private TextMessage sent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        sender = new EmsSender(jmsTemplate);
        ReflectionTestUtils.setField(sender, "queueName", "cola.test");
        ReflectionTestUtils.setField(sender, "timeoutSeconds", 1L);
        ReflectionTestUtils.setField(sender, "timeToLiveMs", 0L);
        ReflectionTestUtils.setField(sender, "dryRun", false);

        session = mock(Session.class);
        replyQueue = mock(TemporaryQueue.class);
        Queue queue = mock(Queue.class);
        producer = mock(MessageProducer.class);
        consumer = mock(MessageConsumer.class);
        sent = mock(TextMessage.class);

        when(session.createTemporaryQueue()).thenReturn(replyQueue);
        when(session.createQueue("cola.test")).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(producer);
        when(session.createConsumer(replyQueue)).thenReturn(consumer);
        when(session.createTextMessage(anyString())).thenReturn(sent);

        when(jmsTemplate.execute(ArgumentMatchers.<SessionCallback<String>>any(), eq(true)))
                .thenAnswer(inv -> ((SessionCallback<String>) inv.getArgument(0)).doInJms(session));
    }

    private TextMessage textReply(String text) throws JMSException {
        TextMessage reply = mock(TextMessage.class);
        when(reply.getText()).thenReturn(text);
        when(reply.getPropertyNames()).thenAnswer(i -> Collections.enumeration(List.of()));
        when(consumer.receive(anyLong())).thenReturn(reply);
        return reply;
    }

    // ------------------------------------------------------------ dry-run

    @Test
    void dryRun_noTocaElEmsYDevuelveUnaRespuestaSimulada() {
        ReflectionTestUtils.setField(sender, "dryRun", true);

        String reply = sender.send("<Envelope/>");

        assertThat(reply).contains("<Code>DRY-RUN</Code>", "dry-run");
        verifyNoInteractions(jmsTemplate);
    }

    // ------------------------------------------------------------ envío normal

    @Test
    void envia_elXmlComoTextMessageConReplyToYDevuelveLaRespuesta() throws Exception {
        textReply("<Response><Code>000</Code></Response>");

        String reply = sender.send("<Envelope/>");

        assertThat(reply).isEqualTo("<Response><Code>000</Code></Response>");
        verify(session).createTextMessage("<Envelope/>");
        verify(sent).setJMSReplyTo(replyQueue);
        verify(producer).setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        verify(producer).send(sent);
    }

    @Test
    void siempreBorraLaColaTemporalDeRespuesta() throws Exception {
        textReply("<ok/>");
        sender.send("<Envelope/>");
        verify(replyQueue).delete();
    }

    @Test
    void esperaLaRespuestaElTiempoConfigurado() throws Exception {
        textReply("<ok/>");
        sender.send("<Envelope/>");
        verify(consumer).receive(1000L);
    }

    @Test
    void conTimeToLive_loAplicaAlProductor() throws Exception {
        ReflectionTestUtils.setField(sender, "timeToLiveMs", 5000L);
        textReply("<ok/>");
        sender.send("<Envelope/>");
        verify(producer).setTimeToLive(5000L);
    }

    @Test
    void sinTimeToLive_noLoAplica() throws Exception {
        textReply("<ok/>");
        sender.send("<Envelope/>");
        verify(producer, never()).setTimeToLive(anyLong());
    }

    // ------------------------------------------------------------ errores

    @Test
    void sinRespuesta_lanzaTimeoutYBorraLaCola() throws Exception {
        when(consumer.receive(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> sender.send("<Envelope/>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error enviando al EMS")
                .hasMessageContaining("Timeout");
        verify(replyQueue).delete();
    }

    @Test
    void falloAlEnviar_seEnvuelveEnRuntimeException() throws Exception {
        doAnswer(inv -> { throw new JMSException("broker caído"); }).when(producer).send(any(Message.class));
        textReply("<ok/>");

        assertThatThrownBy(() -> sender.send("<Envelope/>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("broker caído");
    }

    // ------------------------------------------------------------ tipos de respuesta

    @Test
    void respuestaConCuerpoVacioYPropiedades_seArmaDesdeLasPropiedades() throws Exception {
        TextMessage reply = mock(TextMessage.class);
        when(reply.getText()).thenReturn("");
        when(reply.getPropertyNames()).thenAnswer(i -> Collections.enumeration(List.of("Code", "Description")));
        when(reply.getObjectProperty("Code")).thenReturn("000");
        when(reply.getObjectProperty("Description")).thenReturn("OK");
        when(consumer.receive(anyLong())).thenReturn(reply);

        assertThat(sender.send("<Envelope/>"))
                .isEqualTo("<Response><Code>000</Code><Description>OK</Description></Response>");
    }

    @Test
    void respuestaMapMessage_seConvierteAXml() throws Exception {
        MapMessage reply = mock(MapMessage.class);
        when(reply.getPropertyNames()).thenAnswer(i -> Collections.enumeration(List.of()));
        when(reply.getMapNames()).thenAnswer(i -> Collections.enumeration(List.of("Code")));
        when(reply.getObject("Code")).thenReturn("000");
        when(consumer.receive(anyLong())).thenReturn(reply);

        assertThat(sender.send("<Envelope/>")).isEqualTo("<Response><Code>000</Code></Response>");
    }

    @Test
    void respuestaBytesMessage_seLeeComoUtf8() throws Exception {
        byte[] data = "<Response><Code>5</Code></Response>".getBytes(StandardCharsets.UTF_8);
        BytesMessage reply = mock(BytesMessage.class);
        when(reply.getPropertyNames()).thenAnswer(i -> Collections.enumeration(List.of()));
        when(reply.getBodyLength()).thenReturn((long) data.length);
        doAnswer(inv -> {
            byte[] target = inv.getArgument(0);
            System.arraycopy(data, 0, target, 0, data.length);
            return data.length;
        }).when(reply).readBytes(any(byte[].class));
        when(consumer.receive(anyLong())).thenReturn(reply);

        assertThat(sender.send("<Envelope/>")).isEqualTo("<Response><Code>5</Code></Response>");
    }
}

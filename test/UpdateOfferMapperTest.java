package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOfferMapperTest {

    @Mock UpdateOfferRequestXmlMapper requestXmlMapper;
    @Mock UpdateOfferResponseXmlMapper responseXmlMapper;
    @InjectMocks UpdateOfferMapper mapper;

    @Test
    void toEmsXml_delegaEnElMapperDeRequest() {
        var rq = TestData.request();
        var header = TestData.rqHeader();
        var tz = new MapRequestResultDTO();
        when(requestXmlMapper.toEmsXml(rq, header, "COP_TZCOD_000", tz)).thenReturn("<xml/>");

        assertThat(mapper.toEmsXml(rq, header, "COP_TZCOD_000", tz)).isEqualTo("<xml/>");
    }

    @Test
    void toLogRequestXml_delega() {
        var rq = TestData.request();
        var header = TestData.rqHeader();
        when(requestXmlMapper.toLogXml(rq, header)).thenReturn("<log/>");

        assertThat(mapper.toLogRequestXml(rq, header)).isEqualTo("<log/>");
    }

    @Test
    void toRestResponse_delegaEnElMapperDeRespuesta() {
        UpdateOfferResponse rs = new UpdateOfferResponse();
        when(responseXmlMapper.fromReply("<r/>")).thenReturn(rs);

        assertThat(mapper.toRestResponse("<r/>")).isSameAs(rs);
    }

    @Test
    void toLogResponseXml_delega() {
        UpdateOfferResponse rs = new UpdateOfferResponse();
        when(responseXmlMapper.toLogXml(rs)).thenReturn("<rs/>");

        assertThat(mapper.toLogResponseXml(rs)).isEqualTo("<rs/>");
        verify(responseXmlMapper).toLogXml(rs);
    }
}

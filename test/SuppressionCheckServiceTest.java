package com.banamex.cops.update_offer.suppressed;

import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionResult;
import com.banamex.cops.update_offer.dto.suppression.OperationDetailOutput;
import com.banamex.cops.update_offer.dto.suppression.OperationDetailsDTO;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntitiesListRoot;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AJUSTAR: si tu clase del detalle de entrada no se llama OperationDetailsDTO o no tiene setters
 * (operationName, reqChannel, operationFlag), cambia ese nombre en este archivo.
 */
class SuppressionCheckServiceTest {

    private final SuppressionCheckService service = new SuppressionCheckService();
    private StartRequest request;

    @BeforeEach
    void setUp() {
        request = pedido("CAT", "MX", "UpdateOffer");
    }

    private StartRequest pedido(String canal, String pais, String operacion) {
        StartRequestParams p = new StartRequestParams();
        p.setRequestingChannel(canal);
        p.setCountryCode(pais);
        p.setCurrentOperationName(operacion);
        StartRequest r = new StartRequest();
        r.setRequestParams(p);
        return r;
    }

    private OperationDetailsDTO detalle(String operacion, String canal, String flag) {
        OperationDetailsDTO d = new OperationDetailsDTO();
        d.setOperationName(operacion);
        d.setReqChannel(canal);
        d.setOperationFlag(flag);
        return d;
    }

    private SuppressedEntityDTO entidad(String pais, String canal, String suppFlag, String mode, String source,
                                        OperationDetailsDTO... detalles) {
        SuppressedEntityDTO e = new SuppressedEntityDTO();
        e.setCountryCode(pais);
        e.setChannelId(canal);
        e.setSuppressionFlag(suppFlag);
        e.setOperationFlag("Y");
        e.setMode(mode);
        e.setSourceSystem(source);
        e.setOperationDetails(List.of(detalles));
        return e;
    }

    private SuppressedEntitiesListRoot lista(SuppressedEntityDTO... entidades) {
        SuppressedEntitiesListRoot l = new SuppressedEntitiesListRoot();
        l.setSuppressedEntities(List.of(entidades));
        return l;
    }

    private SuppressedEntityDTO entidadSuprimida() {
        return entidad("MX", "CAT", "Y", "RT", "ECRM", detalle("UpdateOffer", "CAT", "Y"));
    }

    @Test
    void operacionSuprimida_devuelveIndicadorTrueConCodigoYTexto() {
        FilteredSuppressionList r = service.checkIfSuppressed(request, lista(entidadSuprimida()));

        assertThat(r.getResults()).hasSize(1);
        FilteredSuppressionResult res = r.getResults().get(0);
        assertThat(res.getCountryCode()).isEqualTo("MX");
        assertThat(res.getChannelID()).isEqualTo("CAT");
        assertThat(res.getSuppressionFlag()).isEqualTo("Y");
        assertThat(res.getMode()).isEqualTo("RT");
        assertThat(res.getSourceSystem()).isEqualTo("ECRM");
        assertThat(res.getOperationDetails()).hasSize(1);

        OperationDetailOutput d = res.getOperationDetails().get(0);
        assertThat(d.getIndicator()).isEqualTo("true");
        assertThat(d.getResponseCode()).isEqualTo("COP_OPSUP_001");
        assertThat(d.getResponseText()).isEqualTo("The UpdateOffer operation is temporarily unavailable");
        assertThat(d.getCountryCode()).isEqualTo("MX");
    }

    @Test
    void canalDeLaEntidadDistintoAlSolicitado_noCoincide() {
        SuppressedEntityDTO e = entidad("MX", "WEB", "Y", "RT", "ECRM", detalle("UpdateOffer", "CAT", "Y"));
        assertThat(service.checkIfSuppressed(request, lista(e)).getResults()).isEmpty();
    }

    @Test
    void sinCanalSolicitado_noCoincideNiLanzaExcepcion() {
        StartRequest sinCanal = pedido(null, "MX", "UpdateOffer");
        assertThat(service.checkIfSuppressed(sinCanal, lista(entidadSuprimida())).getResults()).isEmpty();
    }

    @Test
    void paisDistinto_noCoincide() {
        SuppressedEntityDTO e = entidad("CO", "CAT", "Y", "RT", "ECRM", detalle("UpdateOffer", "CAT", "Y"));
        assertThat(service.checkIfSuppressed(request, lista(e)).getResults()).isEmpty();
    }

    @Test
    void suppressionFlagDistintoDeY_noCoincide() {
        SuppressedEntityDTO e = entidad("MX", "CAT", "N", "RT", "ECRM", detalle("UpdateOffer", "CAT", "Y"));
        assertThat(service.checkIfSuppressed(request, lista(e)).getResults()).isEmpty();
    }

    @Test
    void modoDistintoDeRT_noCoincide() {
        SuppressedEntityDTO e = entidad("MX", "CAT", "Y", "BATCH", "ECRM", detalle("UpdateOffer", "CAT", "Y"));
        assertThat(service.checkIfSuppressed(request, lista(e)).getResults()).isEmpty();
    }

    @Test
    void sistemaDistintoDeECRM_noCoincide() {
        SuppressedEntityDTO e = entidad("MX", "CAT", "Y", "RT", "OTRO", detalle("UpdateOffer", "CAT", "Y"));
        assertThat(service.checkIfSuppressed(request, lista(e)).getResults()).isEmpty();
    }

    @Test
    void operacionNoContenida_laEntidadCoincidePeroSinDetalles() {
        SuppressedEntityDTO e = entidad("MX", "CAT", "Y", "RT", "ECRM", detalle("OtraOperacion", "CAT", "Y"));

        FilteredSuppressionList r = service.checkIfSuppressed(request, lista(e));

        assertThat(r.getResults()).hasSize(1);
        assertThat(r.getResults().get(0).getOperationDetails()).isEmpty();
    }

    @Test
    void operacionContenidaEnElNombreActual_coincide() {
        StartRequest r2 = pedido("CAT", "MX", "UpdateOfferV2");   // contains(CurrentOperationName, OperationName)
        FilteredSuppressionList r = service.checkIfSuppressed(r2, lista(entidadSuprimida()));
        assertThat(r.getResults().get(0).getOperationDetails()).hasSize(1);
    }

    @Test
    void detalleConOtroCanalODeshabilitado_seDescarta() {
        SuppressedEntityDTO otroCanal = entidad("MX", "CAT", "Y", "RT", "ECRM", detalle("UpdateOffer", "WEB", "Y"));
        SuppressedEntityDTO flagN = entidad("MX", "CAT", "Y", "RT", "ECRM", detalle("UpdateOffer", "CAT", "N"));

        assertThat(service.checkIfSuppressed(request, lista(otroCanal)).getResults().get(0).getOperationDetails()).isEmpty();
        assertThat(service.checkIfSuppressed(request, lista(flagN)).getResults().get(0).getOperationDetails()).isEmpty();
    }

    @Test
    void variasEntidades_soloPasanLasQueCumplen() {
        SuppressedEntityDTO otraOperacion = entidad("MX", "CAT", "Y", "RT", "ECRM", detalle("Otra", "CAT", "Y"));
        SuppressedEntityDTO otroPais = entidad("CO", "CAT", "Y", "RT", "ECRM", detalle("UpdateOffer", "CAT", "Y"));

        FilteredSuppressionList r = service.checkIfSuppressed(request, lista(entidadSuprimida(), otroPais, otraOperacion));

        assertThat(r.getResults()).hasSize(2);   // la suprimida y la que solo coincide en la entidad
    }

    @Test
    void listaVacia_devuelveResultadosVacios() {
        assertThat(service.checkIfSuppressed(request, lista()).getResults()).isEmpty();
    }
}

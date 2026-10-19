import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Equivale al Mapper "CheckIfSuppressed" del proceso BW.
 */
@Service
public class SuppressionCheckService {

    public FilteredSuppressionList checkIfSuppressed(StartRequest request,
                                                     SuppressedEntitiesListRoot suppressed) {
        StartRequestParams p = request.getRequestParams();

        List<FilteredSuppressionResult> results = suppressed.getSuppressedEntities().stream()
                // For-Each predicate: ChannelId = requestingChannel
                .filter(e -> p.getRequestingChannel() != null
                        && p.getRequestingChannel().equals(e.getChannelId()))
                // If
                .filter(e -> p.getCountryCode().equals(e.getCountryCode())
                        && "Y".equals(e.getSuppressionFlag())
                        && "RT".equals(e.getMode())
                        && "ECRM".equals(e.getSourceSystem()))
                .map(e -> toResult(e, p))
                .toList();

        FilteredSuppressionList out = new FilteredSuppressionList();
        out.setResults(results);
        return out;
    }

    private FilteredSuppressionResult toResult(SuppressedEntityDTO e, StartRequestParams p) {
        FilteredSuppressionResult r = new FilteredSuppressionResult();
        r.setCountryCode(e.getCountryCode());
        r.setChannelID(e.getChannelId());
        r.setSuppressionFlag(e.getSuppressionFlag());
        r.setOperationFlag(e.getOperationFlag());
        r.setMode(e.getMode());
        r.setSourceSystem(e.getSourceSystem());

        List<OperationDetailOutput> details = e.getOperationDetails().stream()
                // contains(currentOperationName, OperationName)
                .filter(d -> p.getCurrentOperationName().contains(d.getOperationName()))
                .filter(d -> p.getRequestingChannel().equals(d.getReqChannel())
                        && "Y".equals(d.getOperationFlag()))
                .map(d -> {
                    OperationDetailOutput o = new OperationDetailOutput();
                    o.setOperationName(d.getOperationName());
                    o.setReqChannel(d.getReqChannel());
                    o.setOperationFlag(d.getOperationFlag());
                    o.setCountryCode(p.getCountryCode());
                    o.setIndicator("true");
                    o.setResponseCode("COP_OPSUP_001");
                    o.setResponseText("The " + d.getOperationName()
                            + " operation is temporarily unavailable");
                    return o;
                })
                .toList();

        r.setOperationDetails(details);
        return r;
    }
}

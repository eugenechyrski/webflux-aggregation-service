package com.chyrskiconsulting.accessment.fedex.aggregation.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AggregationResponse(
        Map<String, Optional<Double>> pricing,
        Map<String, Optional<String>> track,
        Map<String, Optional<List<String>>> shipments
) {
}

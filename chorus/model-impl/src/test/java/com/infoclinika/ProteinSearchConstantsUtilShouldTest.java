package com.infoclinika;

import com.google.common.collect.ImmutableMap;
import com.infoclinika.tasks.api.WorkflowWorkerOperations;
import com.infoclinika.tasks.api.workflow.model.SearchParams;
import org.testng.annotations.Test;

import static com.infoclinika.msexplorer.proteinsearch.common.general.ProteinSearchConstants.ProteinSearchConstantsUtil.covertToDoubleView;
import static com.infoclinika.msexplorer.proteinsearch.common.general.ProteinSearchConstants.ProteinSearchConstantsUtil.covertToIntView;
import static org.testng.Assert.assertEquals;

/**
 * @author Andrii Loboda
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class ProteinSearchConstantsUtilShouldTest {

    @Test
    public void convert_double_value_to_int_from_search_params() {
        final String paramKey = "param1Key";
        final SearchParams searchParams = new SearchParams(null, ImmutableMap.of(paramKey, "2.0"));
        final double doubleValue = WorkflowWorkerOperations.extractDoubleParam(searchParams, paramKey);

        final int convertedValue1 = covertToIntView(doubleValue);
        assertEquals(convertedValue1, 200000);

    }

    @Test
    public void convert_double_value_to_int_with_rounding() {
        final int convertedValue2 = covertToIntView(1.99999999999);
        assertEquals(convertedValue2, 199999);

        final int convertedValue1 = covertToIntView(1.4999999999999);
        assertEquals(convertedValue1, 149999);

        final int convertedValue3 = covertToIntView(1.5000000001);
        assertEquals(convertedValue3, 150000);
    }

    @Test
    public void convert_int_value_to_double() {
        final double convertedValue1 = covertToDoubleView(200000);
        assertEquals(convertedValue1, 2.0d);

        final double convertedValue2 = covertToDoubleView(199999);
        assertEquals(convertedValue2, 1.99999);

        final double convertedValue3 = covertToDoubleView(144449);
        assertEquals(convertedValue3, 1.44449);

        final double convertedValue4 = covertToDoubleView(150001);
        assertEquals(convertedValue4, 1.50001);
    }
}

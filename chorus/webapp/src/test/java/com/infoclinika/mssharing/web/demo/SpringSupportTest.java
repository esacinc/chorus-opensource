package com.infoclinika.mssharing.web.demo;

import com.infoclinika.analysis.rest.AnalysisPlatformRestService;
import com.infoclinika.analysis.rest.AnalysisPlatformRestService.AnalysisId;
import com.infoclinika.analysis.rest.AnalysisPlatformRestService.CreateAnalysisTemplateRequest;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Pavel Kaplin
 */
@ContextConfiguration(locations = "classpath:testApplicationContext.cfg.xml")
@Configuration
public class SpringSupportTest {
    /**
     * inspired by http://stackoverflow.com/a/3522070/1338758
     */
    @Before
    public void setUpContext() throws Exception {
        //this is where the magic happens, we actually do "by hand" what the spring runner would do for us,
        // read the JavaDoc for the class bellow to know exactly what it does, the method names are quite accurate though
        TestContextManager testContextManager = new TestContextManager(getClass());
        testContextManager.prepareTestInstance(this);
    }

    @Bean(name = "analysisPlatformRestService")
    public AnalysisPlatformRestService analysisPlatformRestService() {
        final AnalysisPlatformRestService platformRestService = mock(AnalysisPlatformRestService.class);
        when(platformRestService.saveAnalysisTemplate(any(CreateAnalysisTemplateRequest.class)))
                .then(new Answer<AnalysisId>() {
                    @Override
                    public AnalysisId answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final CreateAnalysisTemplateRequest request = (CreateAnalysisTemplateRequest) invocationOnMock.getArguments()[0];
                        return new AnalysisId(request.name, request.name);
                    }
                });
        return platformRestService;
    }
}

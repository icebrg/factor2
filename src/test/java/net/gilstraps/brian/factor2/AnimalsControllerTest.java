package net.gilstraps.brian.factor2;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebAppConfiguration
@ContextConfiguration(classes = AnimalsControllerTest.TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AnimalsControllerTest {

    @Autowired
    WebApplicationContext wac;

    private MockMvc mockMvc;


    @Configuration
    @ComponentScan({ "net.gilstraps.brian.factor2" })
    @EnableWebMvc
    public static class TestConfiguration extends WebMvcConfigurationSupport {
    }

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testTypes() throws Exception {

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();
        JSONArray jsonResponse = new JSONArray(result.getResponse().getContentAsString());
        assertEquals(2,jsonResponse.length());
        List<String> results = new ArrayList<String>();
        for ( int i = 0 ; i < 2 ; i++ ) {
            results.add(jsonResponse.getString(i));
        }
        final List<String> EXPECTED = Arrays.asList("cats","dogs");
        assertEquals(EXPECTED,results);
    }
}

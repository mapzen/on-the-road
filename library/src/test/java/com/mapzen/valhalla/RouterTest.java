package com.mapzen.valhalla;

import com.google.gson.Gson;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;

import static com.mapzen.TestUtils.getFixture;
import static org.fest.assertions.api.Assertions.assertThat;
import retrofit.RestAdapter;

public class RouterTest {
    @Captor ArgumentCaptor<Route> route;
    @Captor ArgumentCaptor<Integer> statusCode;

    Router router;
    MockWebServer server;
    TestHttpHandler httpHandler;

    @Before
    public void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        MockitoAnnotations.initMocks(this);
        double[] loc = new double[] {1.0, 2.0};
        router = new ValhallaRouter().setLocation(loc).setLocation(loc);
        String endpoint = server.getUrl("").toString();
        httpHandler = new TestHttpHandler(endpoint, RestAdapter.LogLevel.NONE);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void shouldDefaultToCar() throws Exception {
        assertThat(router.getJSONRequest().costing).contains("auto");
    }

    @Test
    public void shouldSetToCar() throws Exception {
        router.setDriving();
        assertThat(router.getJSONRequest().costing).contains("auto");
    }

    @Test
    public void shouldSetToBike() throws Exception {
        router.setBiking();
        assertThat(router.getJSONRequest().costing).contains("bicycle");
    }

    @Test
    public void shouldSetToFoot() throws Exception {
        router.setWalking();
        assertThat(router.getJSONRequest().costing).contains("pedestrian");
    }

    @Test
    public void shouldSetToMultimodal() throws Exception {
        router.setMultimodal();
        assertThat(router.getJSONRequest().costing).contains("multimodal");
    }

    @Test
    public void shouldClearLocations() throws Exception {
        double[] loc1 = { 1.0, 2.0 };
        double[] loc2 = { 3.0, 4.0 };
        double[] loc3 = { 5.0, 6.0 };
        Router router = new ValhallaRouter()
                .setLocation(loc1)
                .setLocation(loc2);
        router.clearLocations();
        router.setLocation(loc2);
        router.setLocation(loc3);
        JSON json = router.getJSONRequest();
        assertThat(json.locations[0].lat).doesNotContain("1.0");
        assertThat(json.locations[0].lat).contains("3.0");
        assertThat(json.locations[1].lat).contains("5.0");
    }

    @Test(expected=MalformedURLException.class)
    public void shouldThrowErrorWhenNoLocation() throws Exception {
        new ValhallaRouter().getJSONRequest();
    }

    @Test(expected=MalformedURLException.class)
    public void shouldThrowErrorWhenOnlyOneLocation() throws Exception {
        new ValhallaRouter().setLocation(new double[]{1.0, 1.0}).getJSONRequest();
    }

    @Test
    public void shouldAddLocations() throws Exception {
        double[] loc1 = { 1.0, 2.0 };
        double[] loc2 = { 3.0, 4.0 };
        JSON json = new ValhallaRouter()
                .setLocation(loc1)
                .setLocation(loc2)
                .getJSONRequest();
        assertThat(json.locations[0].lat).contains("1.0");
        assertThat(json.locations[0].lon).contains("2.0");
        assertThat(json.locations[1].lat).contains("3.0");
        assertThat(json.locations[1].lon).contains("4.0");
    }

    @Test
    public void shouldGetRoute() throws Exception {
        final RouteCallback callback = Mockito.mock(RouteCallback.class);
        String routeJson = getFixture("brooklyn_valhalla");
        startServerAndEnqueue(new MockResponse().setBody(routeJson));
        Router router = new ValhallaRouter()
            .setHttpHandler(httpHandler)
            .setLocation(new double[] { 40.659241, -73.983776 })
            .setLocation(new double[] { 40.671773, -73.981115 });
        router.setCallback(callback);
        ((ValhallaRouter) router).run();
        Mockito.verify(callback).success(route.capture());
        assertThat(route.getValue().foundRoute()).isTrue();
    }

    @Test
    public void shouldGetError() throws Exception {
        startServerAndEnqueue(new MockResponse().setResponseCode(500));
        RouteCallback callback = Mockito.mock(RouteCallback.class);
        Router router = new ValhallaRouter()
            .setHttpHandler(httpHandler)
            .setLocation(new double[]{40.659241, -73.983776})
            .setLocation(new double[]{40.671773, -73.981115});
        router.setCallback(callback);
        ((ValhallaRouter) router).run();
        Mockito.verify(callback).failure(statusCode.capture());
        assertThat(statusCode.getValue()).isEqualTo(500);
    }

    @Test
    public void shouldGetNotFound() throws Exception {
        startServerAndEnqueue(new MockResponse().setResponseCode(404));
        RouteCallback callback = Mockito.mock(RouteCallback.class);
        Router router = new ValhallaRouter()
            .setHttpHandler(httpHandler)
            .setLocation(new double[]{40.659241, -73.983776})
            .setLocation(new double[]{40.671773, -73.981115});
        router.setCallback(callback);
        ((ValhallaRouter) router).run();
        Mockito.verify(callback).failure(statusCode.capture());
        assertThat(statusCode.getValue()).isEqualTo(404);
    }

    @Test
    public void shouldGetRouteNotFound() throws Exception {
        startServerAndEnqueue(new MockResponse().setBody(getFixture("unsuccessful")).setResponseCode(400));
        RouteCallback callback = Mockito.mock(RouteCallback.class);
        Router router = new ValhallaRouter()
            .setHttpHandler(httpHandler)
            .setLocation(new double[] { 40.659241, -73.983776 })
            .setLocation(new double[] { 40.671773, -73.981115 });
        router.setCallback(callback);
        ((ValhallaRouter) router).run();
        Mockito.verify(callback).failure(statusCode.capture());
        assertThat(statusCode.getValue()).isEqualTo(400);
    }

    @Test
    public void shouldStoreRawRoute() throws Exception {
        String routeJson = getFixture("brooklyn_valhalla");
        startServerAndEnqueue(new MockResponse().setBody(routeJson));
        RouteCallback callback = Mockito.mock(RouteCallback.class);
        Router router = new ValhallaRouter()
            .setHttpHandler(httpHandler)
            .setLocation(new double[] { 40.659241, -73.983776 })
            .setLocation(new double[] { 40.671773, -73.981115 });
        router.setCallback(callback);
        ((ValhallaRouter) router).run();
        Mockito.verify(callback).success(route.capture());
        assertThat(route.getValue().getRawRoute().toString())
            .isEqualTo(new JSONObject(routeJson).toString());
    }

    @Test
    public void setDistanceUnits_shouldAppendUnitsToJson() throws Exception {
        router.setDistanceUnits(Router.DistanceUnits.MILES);
        assertThat(new Gson().toJson(router.getJSONRequest()))
                .contains("\"directions_options\":{\"units\":\"miles\"}");

        router.setDistanceUnits(Router.DistanceUnits.KILOMETERS);
        assertThat(new Gson().toJson(router.getJSONRequest()))
                .contains("\"directions_options\":{\"units\":\"kilometers\"}");
    }

    @Test
    public void setLocation_shouldAppendName() throws Exception {
        double[] loc = new double[] {1.0, 2.0};
        router = new ValhallaRouter().setLocation(loc)
                .setLocation(loc, "Acme", null, null, null);
        assertThat(new Gson().toJson(router.getJSONRequest()))
                .contains("{\"lat\":\"1.0\",\"lon\":\"2.0\",\"name\":\"Acme\"}");
    }

    @Test
    public void setLocation_shouldNotIncludeNameParamIfNotSet() throws Exception {
        assertThat(new Gson().toJson(router.getJSONRequest()))
                .doesNotContain("\"name\"");
    }

    @Test
    public void setLocation_shouldAppendStreetAddress() throws Exception {
        double[] loc = new double[] {1.0, 2.0};
        router = new ValhallaRouter()
                .setLocation(loc).setLocation(loc, "Acme", "North Main Street", "Doylestown", "PA");
        assertThat(new Gson().toJson(router.getJSONRequest()))
                .contains("{\"lat\":\"1.0\",\"lon\":\"2.0\","
                        + "\"name\":\"Acme\","
                        + "\"street\":\"North Main Street\","
                        + "\"city\":\"Doylestown\","
                        + "\"state\":\"PA\"}");
    }

    @Test
    public void setLocation_shouldIncludeHeading() throws Exception {
        double[] loc = new double[] {1.0, 2.0};
        router = new ValhallaRouter().setLocation(loc, 180).setLocation(loc);
        assertThat(new Gson().toJson(router.getJSONRequest()))
                .contains("{\"lat\":\"1.0\",\"lon\":\"2.0\",\"heading\":\"180\"}");
    }



    @Test
    public void setEndpoint_shouldUpdateBaseRequestUrl() throws Exception {
        startServerAndEnqueue(new MockResponse());
        String endpoint = server.getUrl("/test").toString();
        HttpHandler httpHandler = new HttpHandler(endpoint, RestAdapter.LogLevel.NONE);
        Router router = new ValhallaRouter()
                .setHttpHandler(httpHandler)
                .setLocation(new double[] { 40.659241, -73.983776 })
                .setLocation(new double[] { 40.671773, -73.981115 });
        ((ValhallaRouter) router).run();
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("/test");
    }

    private void startServerAndEnqueue(MockResponse response) throws Exception {
        server.enqueue(response);
    }

}

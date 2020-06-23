/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.hashing.DistributionNonDiscreteRing;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class ClientSelectorTest {
  private static URI URI_1;
  private static URI URI_2;
  private static URI URI_3;
  private static final TrackerClient TRACKER_CLIENT_1 = Mockito.mock(TrackerClient.class);
  private static final TrackerClient TRACKER_CLIENT_2 = Mockito.mock(TrackerClient.class);
  private static final TrackerClient TRACKER_CLIENT_3 = Mockito.mock(TrackerClient.class);
  private static final Map<URI, Integer> DEFAULT_POINTS_MAP = new HashMap<>();
  private static final Ring<URI> DEFAULT_RING;
  private static final Map<URI, TrackerClient> DEFAULT_TRACKER_CLIENT_MAP = new HashMap<>();
  private ClientSelector _clientSelector;

  static {
    try {
      URI_1 = new URI("dummy_uri_1");
      URI_2 = new URI("dummy_uri_2");
      URI_3 = new URI("dummy_uri_3");
    } catch (URISyntaxException e) {
      // do nothing
    }
    Mockito.when(TRACKER_CLIENT_1.getUri()).thenReturn(URI_1);
    Mockito.when(TRACKER_CLIENT_2.getUri()).thenReturn(URI_2);
    Mockito.when(TRACKER_CLIENT_3.getUri()).thenReturn(URI_3);
    DEFAULT_POINTS_MAP.put(URI_1, 60);
    DEFAULT_POINTS_MAP.put(URI_2, 80);
    DEFAULT_POINTS_MAP.put(URI_3, 100);
    DEFAULT_RING = new DistributionNonDiscreteRing<>(DEFAULT_POINTS_MAP);
    DEFAULT_TRACKER_CLIENT_MAP.put(URI_1, TRACKER_CLIENT_1);
    DEFAULT_TRACKER_CLIENT_MAP.put(URI_2, TRACKER_CLIENT_2);
    DEFAULT_TRACKER_CLIENT_MAP.put(URI_3, TRACKER_CLIENT_3);
  }

  @BeforeMethod
  private void setup()
  {
    _clientSelector = new ClientSelector(new RandomHash());
  }

  @Test
  public void testGetTargetHost()
  {
    Request request = Mockito.mock(Request.class);
    RequestContext requestContext = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(requestContext, URI_1);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(request, requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient.getUri(), URI_1);
  }

  @Test
  public void testGetTargetHostNotFound() throws URISyntaxException {
    URI newUri = new URI("new_uri");
    Request request = Mockito.mock(Request.class);
    RequestContext requestContext = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(requestContext, newUri);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(request, requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient, null);
  }

  @Test
  public void testGetHostFromRing()
  {
    Request request = Mockito.mock(Request.class);
    RequestContext requestContext = new RequestContext();

    TrackerClient trackerClient = _clientSelector.getTrackerClient(request, requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertTrue(DEFAULT_TRACKER_CLIENT_MAP.containsKey(trackerClient.getUri()));
  }

  @Test
  public void testAllClientsExcluded()
  {
    Request request = Mockito.mock(Request.class);
    RequestContext requestContext = new RequestContext();
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(requestContext, URI_1);
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(requestContext, URI_2);
    LoadBalancerStrategy.ExcludedHostHints.addRequestContextExcludedHost(requestContext, URI_3);

    TrackerClient trackerClient = _clientSelector.getTrackerClient(request, requestContext, DEFAULT_RING, DEFAULT_TRACKER_CLIENT_MAP);
    assertEquals(trackerClient, null);
  }

  @Test
  public void testRingAndHostInconsistency() throws URISyntaxException {
    Request request = Mockito.mock(Request.class);
    RequestContext requestContext = new RequestContext();
    URI newUri = new URI("new_uri");
    TrackerClient newTrackerClient = Mockito.mock(TrackerClient.class);
    Mockito.when(newTrackerClient.getUri()).thenReturn(newUri);
    Map<URI, TrackerClient> newTrackerClientMap = new HashMap<>();
    newTrackerClientMap.put(newUri, newTrackerClient);

    // Ring and the tracker clients are completely off so that they do not have any overlap
    TrackerClient trackerClient = _clientSelector.getTrackerClient(request, requestContext, DEFAULT_RING, newTrackerClientMap);
    assertEquals(trackerClient, newTrackerClient);
  }
}
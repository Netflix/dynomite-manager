/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.defaultimpl.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.netflix.dynomitemanager.identity.InstanceIdentity;

/**
 * Fake implementation of InstanceIdentity for mocking and unit testing.
 *
 * @author diegopacheco
 *
 */
public class FakeInstanceIdentity extends InstanceIdentity {

	public FakeInstanceIdentity() throws Exception {
		super(null, null, null, null, null, null);
	}

	@Override
	public void init() throws Exception {
		// overrides by-design so it forces not to init the InstanceIdentity.
	}

	@Override
	public String getTokens() {
		return "101134286";
	}

	@Override
	public List<String> getSeeds() throws UnknownHostException {
		List<String> seeds = new ArrayList<>();
		seeds.add("dynomite.us-west-2.prod.myaws.com:8101:us-west-2a:us-west-2:1383429731");
		seeds.add("dynomite.us-west-2.prod.myaws.com:8101:us-west-2b:us-west-2:1383429731");
		seeds.add("dynomite.us-west-2.prod.myaws.com:8101:us-west-2c:us-west-2:1383429731");
		return seeds;
	}

}

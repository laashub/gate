/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.gate.services;

import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.spinnaker.gate.services.commands.HystrixFactory;
import com.netflix.spinnaker.gate.services.internal.ClouddriverService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import retrofit.RetrofitError;

@Component
public class ManifestService {
  private static final String GROUP = "manifests";

  private final ProviderLookupService providerLookupService;
  private final ClouddriverService clouddriverService;

  @Autowired
  public ManifestService(
      ClouddriverService clouddriverService, ProviderLookupService providerLookupService) {
    this.clouddriverService = clouddriverService;
    this.providerLookupService = providerLookupService;
  }

  public Map getManifest(String account, String location, String name) {
    return (Map)
        HystrixFactory.newMapCommand(
                GROUP,
                "getManifest-" + providerLookupService.providerForAccount(account),
                () -> {
                  try {
                    return clouddriverService.getManifest(account, location, name);
                  } catch (RetrofitError re) {
                    if (re.getKind() == RetrofitError.Kind.HTTP
                        && re.getResponse() != null
                        && re.getResponse().getStatus() == 404) {
                      throw new ManifestNotFound(
                          "Unable to find " + name + " in " + account + "/" + location);
                    }
                    throw re;
                  }
                })
            .execute();
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  static class ManifestNotFound extends HystrixBadRequestException {
    ManifestNotFound(String message) {
      super(message);
    }
  }
}

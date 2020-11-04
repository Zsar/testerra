/*
 * Testerra
 *
 * (C) 2020, Mike Reiche, T-Systems Multimedia Solutions GmbH, Deutsche Telekom AG
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {NavigationInstruction, RouteConfig} from "aurelia-router";

export abstract class AbstractViewModel {

    private _routeConfig:RouteConfig;
    private _navInstruction:NavigationInstruction

    activate(
        params: any,
        routeConfig: RouteConfig,
        navInstruction: NavigationInstruction
    ) {
        this._routeConfig = routeConfig;
        this._navInstruction = navInstruction;
    }

    protected updateUrl(params:object) {
        this._navInstruction.router.navigateToRoute(this._routeConfig.name, params, {replace: true});
    }
}

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
import {autoinject, PLATFORM, useView} from 'aurelia-framework';
import {data} from "../../services/report-model";
import {MdcDialog} from '@aurelia-mdc-web/dialog';
import './screenshot-dialog.scss'

@autoinject
@useView(PLATFORM.moduleName('components/screenshots-dialog/screenshots-dialog.html'))
export class ScreenshotsDialog {
    private _screenshots:data.File[];
    private _current:data.File;

    constructor(
        private _dialog: MdcDialog
    ) {

    }

    activate(params:any) {
        this._screenshots = params.screenshots;
        this._current = params.current;
    }

    private _showScreenshot(file:data.File) {
        this._current = file;
    }
}


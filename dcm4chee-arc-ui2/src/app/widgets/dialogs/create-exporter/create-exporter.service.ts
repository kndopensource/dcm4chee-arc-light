import { Injectable } from '@angular/core';
import {J4careHttpService} from "../../../helpers/j4care-http.service";
import {j4care} from "../../../helpers/j4care.service";
import {DeviceConfiguratorService} from "../../../configuration/device-configurator/device-configurator.service";
import {LocalLanguageObject} from "../../../interfaces";
import * as _ from 'lodash-es';

@Injectable()
export class CreateExporterService {

    constructor(
        private $http:J4careHttpService,
        private deviceConfiguratorService:DeviceConfiguratorService
    ) { }

    getDevice = (deviceName) => this.deviceConfiguratorService.getDevice(deviceName);

    getQueue = () => this.$http.get('../queue');

    getExporterDescriptorSchema = () => {
        const currentSavedLanguage = <LocalLanguageObject> JSON.parse(localStorage.getItem('current_language'));
        let deviceSchemaURL = `./assets/schema/exporter.schema.json`;
        if(_.hasIn(currentSavedLanguage,"language.code") && currentSavedLanguage.language.code && currentSavedLanguage.language.code != "en"){
            deviceSchemaURL = `./assets/schema/${currentSavedLanguage.language.code}/exporter.schema.json`;
        }
        return this.$http.get(deviceSchemaURL)
    };

}

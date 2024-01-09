import {ResponseBaseWithData} from "./response.base.data";

export class ResponseValidate extends ResponseBaseWithData<string> {
  childCharacterValidation: boolean = false;
}

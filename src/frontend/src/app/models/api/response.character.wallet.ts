import {ResponseBaseWithData} from "./response.base.data";

export class ResponseCharacterWallet extends ResponseBaseWithData<number> {
  difference: number|null = null;
}

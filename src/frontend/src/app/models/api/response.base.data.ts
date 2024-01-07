import {ResponseBase} from "./response.base";

export class ResponseBaseWithData<T> extends ResponseBase {
  // @ts-ignore
  data: T;
}

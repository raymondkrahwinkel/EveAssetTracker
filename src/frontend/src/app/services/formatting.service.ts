import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FormattingService {

  constructor() { }

  public formatIsk(value: number): string {
    return new Intl.NumberFormat("nl-NL", {
      maximumFractionDigits: 0
    }).format(value);
  }

  public formatDifferenceIsk(value: number): string {
    if(value == 0) {
      return '<span>' + this.formatIsk(value) + '</span>';
    } else if(value < 0) {
      return '<span class="text-danger">' + this.formatIsk(value) + '</span>';
    } else {
      return '<span class="text-success">' + "+" + this.formatIsk(value) + '</span>';
    }
  }
}

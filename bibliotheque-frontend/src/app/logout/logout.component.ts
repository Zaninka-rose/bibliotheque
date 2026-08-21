import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'app-logout',
    templateUrl: './logout.component.html',
    styleUrls: ['./logout.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class LogoutComponent implements OnInit {

  constructor(private router: Router) {

  }

  ngOnInit() {
  }

}
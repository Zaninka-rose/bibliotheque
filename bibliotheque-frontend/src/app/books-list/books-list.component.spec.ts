import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { BooksListComponent } from './books-list.component';
import { BooksService } from '../_service/books.service';
import { Router } from '@angular/router';

describe('BooksListComponent', () => {
  let component: BooksListComponent;
  let fixture: ComponentFixture<BooksListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BooksListComponent ],
      providers: [
        { provide: BooksService, useValue: { getBooksList: () => of([]), deleteBook: () => of({}) } },
        { provide: Router, useValue: { navigate: () => {} } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BooksListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

import { BookDetailsComponent } from './book-details.component';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UsersService } from '../_service/users.service';

describe('BookDetailsComponent', () => {
  let component: BookDetailsComponent;
  let fixture: ComponentFixture<BookDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BookDetailsComponent ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { bookId: 1 } } } },
        { provide: BooksService, useValue: { getBookById: () => of({}) } },
        { provide: BorrowService, useValue: { getBookBorrowHistory: () => of([]) } },
        { provide: UsersService, useValue: { getUserById: () => of({}) } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

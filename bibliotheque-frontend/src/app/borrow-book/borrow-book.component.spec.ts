import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { BorrowBookComponent } from './borrow-book.component';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';

describe('BorrowBookComponent', () => {
  let component: BorrowBookComponent;
  let fixture: ComponentFixture<BorrowBookComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ BorrowBookComponent ],
      providers: [
        { provide: BooksService, useValue: { getBooksList: () => of([]) } },
        { provide: BorrowService, useValue: { borrowBook: () => of({}) } },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BorrowBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

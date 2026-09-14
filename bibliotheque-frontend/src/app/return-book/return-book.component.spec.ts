import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ReturnBookComponent } from './return-book.component';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';

describe('ReturnBookComponent', () => {
  let component: ReturnBookComponent;
  let fixture: ComponentFixture<ReturnBookComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ReturnBookComponent ],
      providers: [
        { provide: BooksService, useValue: { getBooksList: () => of([]) } },
        { provide: BorrowService, useValue: { getBooksBorrowedByUser: () => of([]), returnBook: () => of({}) } },
        { provide: UserAuthService, useValue: { getUserId: () => 1 } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReturnBookComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

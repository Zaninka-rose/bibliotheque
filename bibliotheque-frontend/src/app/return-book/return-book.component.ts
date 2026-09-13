import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
    selector: 'app-return-book',
    templateUrl: './return-book.component.html',
    styleUrls: ['./return-book.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReturnBookComponent implements OnInit {

  books: Books[];
  borrow: Borrow[];
  loading = false;
  feedback = '';
  feedbackError = '';

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
    this.getBooksByUser();
  }

  private getBooks() {
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
    });
  }


  private getBooksByUser() {
    this.loading = true;
    this.borrowService.getBooksBorrowedByUser(this.userId).subscribe(data => {
      this.borrow = data;
      this.loading = false;
    }, () => {
      this.loading = false;
    });
  }

  brw: Borrow = new Borrow();
  public returnBook(borrowId: number) {
    this.feedback = '';
    this.feedbackError = '';
    this.brw.borrowId = borrowId;
    this.borrowService.returnBook(this.brw).subscribe(() => {
      this.feedback = 'Retour enregistré. Merci !';
      this.getBooksByUser();
    },
    () => {
      this.feedbackError = 'Le retour n\'a pas pu être enregistré. Réessayez.';
    });
  }
}

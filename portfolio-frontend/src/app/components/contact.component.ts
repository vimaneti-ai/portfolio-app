import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ApiService } from '../services/api.service';

/**
 * Contact form using Angular reactive forms.
 * Validation here mirrors the backend rules (and the column lengths in MySQL),
 * which is the same lesson from the KFin bug: the front end, the model, and the
 * database all have to agree on field limits.
 */
@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.css'],
})
export class ContactComponent {

  form: FormGroup;
  submitting = false;
  successMessage = '';
  errorMessage = '';

  constructor(private fb: FormBuilder, private api: ApiService) {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      message: ['', [Validators.required, Validators.maxLength(2000)]],
    });
  }

  // Convenience getters for the template.
  get firstName() { return this.form.get('firstName'); }
  get lastName() { return this.form.get('lastName'); }
  get email() { return this.form.get('email'); }
  get message() { return this.form.get('message'); }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();   // shows all error messages at once
      return;
    }

    this.submitting = true;
    this.api.sendMessage(this.form.value).subscribe({
      next: (res) => {
        this.successMessage = res.message;
        this.form.reset();
        this.submitting = false;
      },
      error: () => {
        this.errorMessage = 'Something went wrong. Please try again in a moment.';
        this.submitting = false;
      },
    });
  }
}

import { Stepper as MantineStepper, useMatches, VisuallyHidden } from '@mantine/core';

export interface StepperProps {
  steps: { label: string; description?: string }[];
  /** Índice del paso actual, base 0. */
  current: number;
  'aria-label': string;
}

/** Indicador informativo de avance: vertical en teléfono y horizontal desde tablet. */
export function Stepper({ steps, current, 'aria-label': ariaLabel }: StepperProps) {
  const orientation = useMatches<'vertical' | 'horizontal'>({ base: 'vertical', sm: 'horizontal' });

  return (
    <MantineStepper
      active={current}
      orientation={orientation}
      allowNextStepsSelect={false}
      aria-label={ariaLabel}
    >
      {steps.map((step, i) => (
        <MantineStepper.Step
          key={step.label}
          label={step.label}
          description={step.description}
          aria-current={i === current ? 'step' : undefined}
          // Los pasos completados se anuncian como tales; la marca es solo visual.
          completedIcon={i < current ? <VisuallyHidden>completado</VisuallyHidden> : undefined}
        />
      ))}
    </MantineStepper>
  );
}

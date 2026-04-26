using ClimbBetter.Application.DTOs;
using ClimbBetter.Application.DTOs.TrainingSessions;
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingSessions.CreateTrainingSession;


public class CreateTrainingSessionCommandHandler 
    : IRequestHandler<CreateTrainingSessionCommand, Guid>
{

    public Task<Guid> Handle( 
        CreateTrainingSessionCommand request, 
        CancellationToken cancellationToken)
    {
        return Task.FromResult(Guid.NewGuid());
    }
}